package com.example.gadgetmover.data

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.gadgetmover.model.Address
import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.ChatThread
import com.example.gadgetmover.model.CheckoutDetails
import com.example.gadgetmover.model.DepositStatus
import com.example.gadgetmover.model.FilterState
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.Message
import com.example.gadgetmover.model.MessageMetadata
import com.example.gadgetmover.model.MessageType
import com.example.gadgetmover.model.Notification
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.OrderStatus
import com.example.gadgetmover.model.OtpPurpose
import com.example.gadgetmover.model.PaymentRecordStatus
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductStatus
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.model.Review
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.model.ReturnMethod
import com.example.gadgetmover.model.ReturnRequest
import com.example.gadgetmover.model.ReturnRequestType
import com.example.gadgetmover.model.SortOption
import com.example.gadgetmover.model.User
import com.example.gadgetmover.model.WalletTransaction
import com.example.gadgetmover.model.WalletTransactionType
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.Identity
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order as PostgrestOrder
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

/**
 * In-memory repository backed by live Supabase data (cached offline).
 * Lists start empty on fresh install and are populated via remote fetch.
 */
object ProductRepository {

    private val _products = mutableStateListOf<Product>()
    val products: List<Product> get() = _products

    /**
     * Seeds the in-memory list from [ProductCache] before any network call — called once from
     * `MainActivity.onCreate`, synchronously and before Compose even starts, so Home's very first
     * composition already has last session's listings to show instead of an empty screen while
     * [refreshFromRemote] is still in flight.
     */
    fun loadCache(cached: List<Product>) {
        if (_products.isEmpty()) {
            _products.addAll(cached)
        }
    }

    private val savedIds = mutableStateListOf<String>()

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    /** Every listing except the current user's own and anything already SOLD — what Home/Explore/search should show, since a seller shouldn't be offered their own inventory to browse or buy, and a sold BUY/BOTH listing is gone (a `Product` is always exactly one physical unit). [products]/[myListings]/[getById] stay unfiltered themselves — [getById] still needs to load a sold product for product-detail/order-detail to render its "Sold" state, and callers that want only active listings (e.g. `MyListingsScreen`) filter [myListings]'s result themselves. */
    val browsable: List<Product> get() = _products.filterNot { it.sellerId == currentUserId() || it.status == ProductStatus.SOLD }

    fun getById(id: String): Product? = _products.find { it.id == id }

    fun getFeatured(): List<Product> = browsable.filter { it.isFeatured }

    fun getByCategory(category: com.example.gadgetmover.model.ProductCategory): List<Product> =
        _products.filter { it.category == category }

    fun isSaved(productId: String): Boolean = savedIds.contains(productId)

    @Serializable
    private data class SavedItemRow(
        @SerialName("user_id") val userId: String,
        @SerialName("product_id") val productId: String
    )

    /** Pulls the current user's saved-item ids from `saved_items` into the local [savedIds] cache — [isSaved] stays a fast synchronous read backed by this. Call after login/session-restore. */
    suspend fun refreshSavedIds() {
        val uid = currentUserId() ?: return
        try {
            val rows = supabase.from("saved_items").select { filter { eq("user_id", uid) } }.decodeList<SavedItemRow>()
            savedIds.clear()
            savedIds.addAll(rows.map { it.productId })
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    suspend fun toggleSaved(productId: String): Boolean {
        val uid = currentUserId() ?: return false
        return try {
            if (savedIds.contains(productId)) {
                supabase.from("saved_items").delete { filter { eq("user_id", uid); eq("product_id", productId) } }
                savedIds.remove(productId)
            } else {
                supabase.from("saved_items").insert(SavedItemRow(userId = uid, productId = productId))
                savedIds.add(productId)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSaved(): List<Product> = _products.filter { savedIds.contains(it.id) }

    /**
     * Fetches every live listing from the `products` table, joins in seller display info from
     * `profiles`, and replaces the observed [products] list in place — every screen already
     * reading [products]/[getById]/[search]/etc. recomposes automatically, no call-site changes
     * needed. On any failure the existing (seeded mock) data is left untouched rather than
     * blanking the screen.
     */
    suspend fun refreshFromRemote() {
        try {
            val rows = supabase.from("products").select().decodeList<ProductRow>()
            if (rows.isEmpty()) return
            val sellerIds = rows.map { it.sellerId }.distinct()
            val profiles = try {
                supabase.from("profiles").select {
                    filter { isIn("id", sellerIds) }
                }.decodeList<ProfileRow>().associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }
            val fresh = rows.map { row ->
                val profile = profiles[row.sellerId]
                row.toProduct(
                    sellerName = profile?.username.orEmpty(),
                    sellerRating = profile?.rating ?: 0f,
                    isSellerVerified = profile?.isVerified ?: false,
                    sellerAvatarUrl = profile?.avatarUrl.orEmpty()
                )
            }
            _products.clear()
            _products.addAll(fresh)
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    private const val PRODUCT_IMAGES_BUCKET = "product-images"

    /**
     * Uploads each picked photo to the `product-images` Storage bucket under
     * `{sellerId}/{productId}/{index}.jpg` and returns the public URLs of whichever ones
     * succeeded — a single failed upload (e.g. a transient network drop) doesn't block
     * publishing the listing with the rest. Returns an empty list if [uris] is empty; that's a
     * valid listing state, not an error.
     */
    suspend fun uploadProductImages(
        sellerId: String,
        productId: String,
        uris: List<Uri>,
        contentResolver: ContentResolver
    ): List<String> {
        val bucket = supabase.storage.from(PRODUCT_IMAGES_BUCKET)
        val urls = mutableListOf<String>()

        uris.forEachIndexed { index, uri ->
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEachIndexed
                val path = "$sellerId/$productId/$index.jpg"
                bucket.upload(path, bytes) { upsert = true }
                urls.add(bucket.publicUrl(path))
            } catch (e: Exception) {
                // Skip this photo — the rest of the listing still publishes.
            }
        }
        return urls
    }

    /** Inserts [product] into the `products` table, then reflects it locally on success. */
    suspend fun addProduct(product: Product): Boolean {
        return try {
            supabase.from("products").insert(product.toProductRow())
            _products.add(0, product)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Updates an existing listing in the `products` table, then reflects it locally on success — used by the Edit Listing flow. */
    suspend fun updateProduct(product: Product): Boolean {
        return try {
            supabase.from("products").update(product.toProductRow()) { filter { eq("id", product.id) } }
            val index = _products.indexOfFirst { it.id == product.id }
            if (index >= 0) _products[index] = product
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Deletes the product from the `products` table, then reflects it locally on success. */
    suspend fun removeProduct(productId: String): Boolean {
        return try {
            supabase.from("products").delete { filter { eq("id", productId) } }
            _products.removeAll { it.id == productId }
            true
        } catch (e: Exception) {
            false
        }
    }

    @Serializable
    private data class MarkProductSoldParams(@SerialName("p_order_id") val orderId: String)

    /**
     * Marks [productId] SOLD via `mark_product_sold` — called right after a BUY order for it is
     * created (never a direct client UPDATE; RLS only lets the seller update `products`). A
     * failure here is logged and swallowed rather than surfaced: the order itself already
     * succeeded, and a missed status flag must never make an already-completed purchase look
     * like it failed.
     */
    suspend fun markSold(orderId: String, productId: String) {
        try {
            supabase.postgrest.rpc("mark_product_sold", MarkProductSoldParams(orderId = orderId))
            val index = _products.indexOfFirst { it.id == productId }
            if (index >= 0) _products[index] = _products[index].copy(status = ProductStatus.SOLD)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "markSold($orderId, $productId) failed", e)
        }
    }

    fun myListings(sellerId: String): List<Product> = _products.filter { it.sellerId == sellerId }

    fun search(filter: FilterState): List<Product> {
        var results = browsable

        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            results = results.filter {
                it.title.lowercase().contains(q) ||
                    it.brandLabel().lowercase().contains(q) ||
                    it.category.label.lowercase().contains(q)
            }
        }

        filter.transactionType?.let { type ->
            results = results.filter {
                it.listingType == type || it.listingType == ListingType.BOTH
            }
        }

        if (filter.categories.isNotEmpty()) {
            results = results.filter { it.category in filter.categories }
        }

        if (filter.conditions.isNotEmpty()) {
            results = results.filter { it.condition in filter.conditions }
        }

        results = results.filter { product ->
            val effectivePrice = product.price ?: product.rentalRatePerDay ?: 0.0
            effectivePrice >= filter.priceRange.start && effectivePrice <= filter.priceRange.endInclusive
        }

        results = when (filter.sortBy) {
            SortOption.RELEVANCE -> results
            SortOption.PRICE_LOW_HIGH -> results.sortedBy { it.price ?: it.rentalRatePerDay ?: 0.0 }
            SortOption.PRICE_HIGH_LOW -> results.sortedByDescending { it.price ?: it.rentalRatePerDay ?: 0.0 }
            SortOption.NEWEST -> results
            SortOption.RATING -> results.sortedByDescending { it.sellerRating }
        }

        return results
    }
}

/** Account details held between "Register" and a successful OTP check. */
data class PendingRegistration(val name: String, val userId: String, val email: String, val password: String, val phone: String)

enum class OtpSendResult { SENT, EMAIL_ALREADY_REGISTERED, USER_ID_TAKEN, EMAIL_NOT_FOUND, EMAIL_DELIVERY_FAILED }

/**
 * [fallbackCode] is only populated for [OtpPurpose.REGISTRATION] when [result] is
 * [OtpSendResult.EMAIL_DELIVERY_FAILED] — it lets the UI show the code directly as a
 * fallback so a failed Resend delivery is never an unrecoverable dead end. For
 * [OtpPurpose.FORGOT_PASSWORD] the code is generated and emailed entirely inside Supabase
 * Auth, so the app never sees it and this stays null even on delivery failure.
 */
data class OtpSendOutcome(val result: OtpSendResult, val fallbackCode: String? = null)

enum class OtpVerifyResult { SUCCESS, INCORRECT, EXPIRED, NOT_FOUND }

enum class ChangePasswordResult { SUCCESS, INCORRECT_CURRENT, FAILED }

/** [Failed.reason] is shown to the user as-is (see [AuthRepository.completeRegistration]) so a real failure — most often a Supabase project misconfiguration, not a bug in this flow — is diagnosable instead of a generic "something went wrong". */
sealed class CompleteRegistrationResult {
    data object Success : CompleteRegistrationResult()
    data class Failed(val reason: String) : CompleteRegistrationResult()
}

/**
 * Backed by Supabase Auth (`auth.users` owns credentials) + the `profiles` Postgrest table
 * (public-readable profile data). Registration and forgot-password share one OTP-gated UX,
 * but the two purposes are handled by deliberately different mechanisms under the hood:
 *
 * - [OtpPurpose.REGISTRATION]: no Supabase account exists yet, so there's no privileged
 *   action being gated — the app keeps generating its own 6-digit code and emailing it via
 *   [EmailService]/Resend exactly as before, then only calls Supabase's `signUp` once that
 *   code is confirmed.
 * - [OtpPurpose.FORGOT_PASSWORD]: this changes an *existing* account's password, which the
 *   client can never safely do with just the anon key and a self-generated code (that would
 *   mean anyone who can receive an email at an address could reset that account's password
 *   without Supabase ever validating the OTP itself). So this purpose routes through
 *   Supabase Auth's own `resetPasswordForEmail` + `verifyEmailOtp(type = Recovery)` flow —
 *   Supabase generates, emails, and validates the code, and only hands the app a signed-in
 *   recovery session once it's confirmed correct. (Resend can still be the email carrier for
 *   this flow too — wire it in as the project's outbound SMTP provider under Supabase
 *   Dashboard → Authentication → SMTP Settings — but the code itself is Supabase's, not ours.)
 */
object AuthRepository {

    // No account is logged in by default — every screen must handle a null current user.
    val currentUser = mutableStateOf<User?>(null)
    val isLoggedIn = mutableStateOf(false)

    /**
     * False until [restoreSession] has finished checking for a persisted session at app start.
     * Any UI that shows a "please log in" state based on [isLoggedIn] must also require this —
     * [isLoggedIn] defaults to false before the restore completes, so checking it alone made an
     * already-logged-in user briefly see a login prompt on every cold app start.
     */
    val sessionRestored = mutableStateOf(false)

    private const val OTP_VALIDITY_MILLIS = 5 * 60 * 1000L

    private var pendingRegistration: PendingRegistration? = null
    private var otpCode: String? = null
    private var otpEmail: String? = null
    private var otpPurpose: OtpPurpose? = null
    private var otpExpiresAtMillis: Long = 0L

    /** Call once at app start (e.g. a top-level `LaunchedEffect(Unit)`) to rehydrate a persisted session. */
    suspend fun restoreSession() {
        try {
            supabase.auth.awaitInitialization()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return
            loadProfileIntoCurrentUser(userId, supabase.auth.currentUserOrNull()?.email.orEmpty())
        } finally {
            sessionRestored.value = true
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val userId = supabase.auth.currentUserOrNull()?.id ?: return false
            loadProfileIntoCurrentUser(userId, email.trim())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Called right after [io.github.jan.supabase.compose.auth.composable.NativeSignInResult.Success]
     * from the "Continue with Google" button (`LoginScreen`) — the Credential Manager flow has
     * already established the Supabase session by that point (its default `onIdToken` callback
     * calls `auth.signInWith(IDToken)` itself), so this just loads the resulting profile the same
     * way [restoreSession]/[login] do. [User.hasPassword] on the loaded profile reflects whether
     * this was a first-time Google sign-up (see `handle_new_user()` in schema.sql) or a returning
     * Google user who already created a password since.
     */
    suspend fun completeGoogleSignIn(): Boolean {
        val user = supabase.auth.currentUserOrNull() ?: return false
        loadProfileIntoCurrentUser(user.id, user.email.orEmpty())
        return true
    }

    /**
     * Sets a real Gadget Mover password for an account that doesn't have one yet (a Google
     * sign-up — see [User.hasPassword]). Unlike [changePassword], there's no current password to
     * verify: the user's already-live Google-established session is itself the proof of identity,
     * so this goes straight to `auth.updateUser`.
     */
    suspend fun setInitialPassword(newPassword: String): Boolean {
        val userId = currentUser.value?.id ?: return false
        return try {
            supabase.auth.updateUser { password = newPassword }
            supabase.from("profiles").update({
                set("has_password", true)
            }) {
                filter { eq("id", userId) }
            }
            currentUser.value = currentUser.value?.copy(hasPassword = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * The caller's linked auth identities (Google, email, ...) straight off the live GoTrue
     * session — this is session state, not app profile data, so it isn't mirrored onto [User]
     * the way [User.hasPassword] is. Used by Account Information to decide whether to offer
     * unlinking a Google account.
     */
    suspend fun currentIdentities(): List<Identity> =
        supabase.auth.currentUserOrNull()?.identities.orEmpty()

    /**
     * Unlinks [identityId] (an [Identity.identityId]) from the current account — e.g. detaching
     * Google after the user has a password to fall back on. GoTrue itself refuses to unlink an
     * account's only identity, so this can legitimately fail for a Google-only user with no
     * password yet; the caller should guide them to create one first in that case.
     */
    suspend fun unlinkIdentity(identityId: String): Boolean {
        return try {
            supabase.auth.unlinkIdentity(identityId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Re-authenticates the already-logged-in current user with [password] — used to gate
     * sensitive actions (paying with the wallet, withdrawing) behind a password prompt without
     * a separate "verify password" endpoint. A successful call is just a normal sign-in for the
     * same account, so it also harmlessly refreshes the session.
     */
    suspend fun verifyPassword(password: String): Boolean {
        val email = currentUser.value?.email ?: return false
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            // Already signed out / offline — clear local state regardless.
        }
        isLoggedIn.value = false
        currentUser.value = null
        WalletRepository.clear()
    }

    /** Persists an edited profile so it survives a subsequent logout/login. */
    suspend fun updateCurrentUser(updated: User): Boolean {
        return try {
            supabase.from("profiles").update({
                set("username", updated.name)
                set("user_id", updated.userId)
                set("phone_number", updated.phone)
                set("location", updated.location)
                set("avatar_url", updated.avatarUrl)
            }) {
                filter { eq("id", updated.id) }
            }
            currentUser.value = updated
            true
        } catch (e: Exception) {
            false
        }
    }

    /** True if [userId] is free to take — used before saving an edited handle, so a clash surfaces as a specific field error instead of a generic save failure from the DB's unique index. [excludingUserId] should be the editor's own account id, so re-saving your own unchanged handle doesn't read as taken. */
    suspend fun isUserIdAvailable(userId: String, excludingUserId: String? = null): Boolean {
        val owner = findProfileByUserId(userId)?.id
        return owner == null || owner == excludingUserId
    }

    /** Matches [query] against both the display name and the unique handle — used by Explore's "Search items or user" box to surface matching sellers alongside product results. Empty/blank queries return nothing rather than every profile. */
    suspend fun searchUsers(query: String): List<User> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        return try {
            supabase.from("profiles").select {
                filter {
                    or {
                        ilike("username", "%$trimmed%")
                        ilike("user_id", "%$trimmed%")
                    }
                }
                limit(10)
            }.decodeList<ProfileRow>().map { it.toUser() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * For [OtpPurpose.REGISTRATION]: generates a fresh 6-digit OTP, emails it via
     * [EmailService]/Resend, and stores it (with a 5-minute expiry) for [verifyOtp].
     * For [OtpPurpose.FORGOT_PASSWORD]: delegates entirely to Supabase Auth's
     * `resetPasswordForEmail`, which generates and emails its own OTP/recovery code.
     */
    suspend fun sendOtp(
        email: String,
        purpose: OtpPurpose,
        registration: PendingRegistration? = null
    ): OtpSendOutcome {
        val trimmedEmail = email.trim()
        return when (purpose) {
            OtpPurpose.REGISTRATION -> sendRegistrationOtp(trimmedEmail, registration)
            OtpPurpose.FORGOT_PASSWORD -> sendForgotPasswordOtp(trimmedEmail)
        }
    }

    private suspend fun sendRegistrationOtp(email: String, registration: PendingRegistration?): OtpSendOutcome {
        if (findProfileByEmail(email) != null) {
            return OtpSendOutcome(OtpSendResult.EMAIL_ALREADY_REGISTERED)
        }
        val effectiveUserId = registration?.userId ?: pendingRegistration?.userId.orEmpty()
        if (findProfileByUserId(effectiveUserId) != null) {
            return OtpSendOutcome(OtpSendResult.USER_ID_TAKEN)
        }
        // A resend keeps whatever pending details the first send cached if none are passed.
        pendingRegistration = registration ?: pendingRegistration

        val code = (0..999999).random().toString().padStart(6, '0')
        otpCode = code
        otpEmail = email
        otpPurpose = OtpPurpose.REGISTRATION
        otpExpiresAtMillis = System.currentTimeMillis() + OTP_VALIDITY_MILLIS

        val delivered = EmailService.sendOtpEmail(email, code, "verify your new Gadget Mover account")
        return if (delivered) {
            OtpSendOutcome(OtpSendResult.SENT)
        } else {
            OtpSendOutcome(OtpSendResult.EMAIL_DELIVERY_FAILED, fallbackCode = code)
        }
    }

    private suspend fun sendForgotPasswordOtp(email: String): OtpSendOutcome {
        if (findProfileByEmail(email) == null) {
            return OtpSendOutcome(OtpSendResult.EMAIL_NOT_FOUND)
        }
        otpEmail = email
        otpPurpose = OtpPurpose.FORGOT_PASSWORD
        return try {
            supabase.auth.resetPasswordForEmail(email)
            OtpSendOutcome(OtpSendResult.SENT)
        } catch (e: Exception) {
            // Supabase owns this email — there is no locally-generated code to fall back to.
            OtpSendOutcome(OtpSendResult.EMAIL_DELIVERY_FAILED)
        }
    }

    suspend fun verifyOtp(email: String, purpose: OtpPurpose, code: String): OtpVerifyResult {
        if (otpPurpose != purpose || !otpEmail.equals(email.trim(), ignoreCase = true)) return OtpVerifyResult.NOT_FOUND
        return when (purpose) {
            OtpPurpose.REGISTRATION -> verifyRegistrationOtp(code)
            OtpPurpose.FORGOT_PASSWORD -> verifyForgotPasswordOtp(email.trim(), code)
        }
    }

    private fun verifyRegistrationOtp(code: String): OtpVerifyResult {
        val storedCode = otpCode ?: return OtpVerifyResult.NOT_FOUND
        if (System.currentTimeMillis() > otpExpiresAtMillis) return OtpVerifyResult.EXPIRED
        if (code != storedCode) return OtpVerifyResult.INCORRECT
        return OtpVerifyResult.SUCCESS
    }

    private suspend fun verifyForgotPasswordOtp(email: String, code: String): OtpVerifyResult {
        return try {
            supabase.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email, token = code)
            OtpVerifyResult.SUCCESS
        } catch (e: Exception) {
            OtpVerifyResult.INCORRECT
        }
    }

    /**
     * Creates the account cached by [sendOtp] and starts a session. Call only after [verifyOtp]
     * returns SUCCESS.
     *
     * `signUpWith` only returns an active session immediately if the Supabase project's own
     * "Confirm email" setting is turned off — this app already gates signup behind its own
     * emailed OTP, so requiring Supabase's separate confirmation link on top of that is redundant
     * and breaks this exact call (there's no session to read [supabase.auth.currentUserOrNull]
     * from, and the [supabase.from] profile update below would fail RLS with no authenticated
     * user either). The `signInWith` fallback covers the case where the session merely hadn't
     * propagated yet; if the project genuinely requires confirmation, that will fail too, in
     * which case the real fix is disabling it in Dashboard → Authentication → Sign In / Providers
     * → Email.
     */
    suspend fun completeRegistration(): CompleteRegistrationResult {
        val pending = pendingRegistration
            ?: return CompleteRegistrationResult.Failed("No pending registration found — please start over.")
        return try {
            supabase.auth.signUpWith(Email) {
                email = pending.email
                password = pending.password
            }
            var userId = supabase.auth.currentUserOrNull()?.id
            if (userId == null) {
                try {
                    supabase.auth.signInWith(Email) {
                        email = pending.email
                        password = pending.password
                    }
                } catch (signInError: Exception) {
                    return CompleteRegistrationResult.Failed(
                        "Account created but couldn't sign in automatically: ${signInError.message ?: "unknown error"}. " +
                            "If your Supabase project has \"Confirm email\" turned on, turn it off " +
                            "(Dashboard → Authentication → Sign In / Providers → Email) — this app already " +
                            "verifies the email with its own code, so that setting conflicts with it."
                    )
                }
                userId = supabase.auth.currentUserOrNull()?.id
            }
            if (userId == null) {
                return CompleteRegistrationResult.Failed("Account created but no session was returned. Check your Supabase project's email confirmation setting.")
            }
            supabase.from("profiles").update({
                set("username", pending.name)
                set("user_id", pending.userId)
                set("phone_number", pending.phone)
            }) {
                filter { eq("id", userId) }
            }
            loadProfileIntoCurrentUser(userId, pending.email)
            clearOtp()
            CompleteRegistrationResult.Success
        } catch (e: Exception) {
            CompleteRegistrationResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Sets a new password for the account whose recovery OTP was just confirmed by
     * [verifyOtp] — that call already established an authenticated recovery session, so this
     * updates *that* session's user rather than looking [email] up separately. Signs the
     * recovery session back out afterward so the user returns to a normal login with their
     * new password, matching the rest of the app's post-reset navigation to the Login screen.
     */
    suspend fun resetPassword(email: String, newPassword: String): Boolean {
        return try {
            supabase.auth.updateUser { password = newPassword }
            supabase.auth.signOut()
            clearOtp()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Changes the password for the currently logged-in account. [User.password] is never
     * populated locally (GoTrue owns credentials, not `profiles`), so — unlike the old mock
     * repository — [currentPassword] can't be compared client-side. Instead it's verified by
     * re-authenticating with it before applying the change, exactly what a server would do.
     */
    suspend fun changePassword(email: String, currentPassword: String, newPassword: String): ChangePasswordResult {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                password = currentPassword
            }
        } catch (e: Exception) {
            return ChangePasswordResult.INCORRECT_CURRENT
        }
        return try {
            supabase.auth.updateUser { password = newPassword }
            ChangePasswordResult.SUCCESS
        } catch (e: Exception) {
            ChangePasswordResult.FAILED
        }
    }

    /** Fetches another user's public profile (name, avatar, rating, join date, location) by id — used to render [com.example.gadgetmover.screen.profile.SellerProfileScreen] for whichever seller the viewer taps into, as opposed to [currentUser] which only ever holds the signed-in account. */
    suspend fun fetchProfile(userId: String): User? {
        return try {
            supabase.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileRow>()?.toUser()
        } catch (e: Exception) {
            null
        }
    }

    /** Re-fetches the signed-in user's own profile row from the server and replaces [currentUser] with it — for screens (Account Information) that need the authoritative post-change state rather than trusting an optimistic local patch. A logged-out call is a no-op. */
    suspend fun refreshCurrentUser() {
        val id = currentUser.value?.id ?: return
        loadProfileIntoCurrentUser(id, currentUser.value?.email.orEmpty())
    }

    private suspend fun findProfileByEmail(email: String): ProfileRow? {
        return try {
            supabase.from("profiles").select {
                filter { eq("email", email) }
            }.decodeSingleOrNull<ProfileRow>()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun findProfileByUserId(userId: String): ProfileRow? {
        if (userId.isBlank()) return null
        return try {
            supabase.from("profiles").select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<ProfileRow>()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadProfileIntoCurrentUser(userId: String, fallbackEmail: String) {
        val row = try {
            supabase.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileRow>()
        } catch (e: Exception) {
            null
        }
        currentUser.value = (row ?: ProfileRow(id = userId, email = fallbackEmail)).toUser()
        isLoggedIn.value = true
    }

    private fun clearOtp() {
        otpCode = null
        otpEmail = null
        otpPurpose = null
        pendingRegistration = null
    }
}

object OrderRepository {

    private val _orders = mutableStateListOf<Order>()
    val orders: List<Order> get() = _orders

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    /**
     * Fetches every order visible to the logged-in user — RLS already scopes the `orders`
     * table to rows where they're the buyer or the seller — and replaces the observed
     * [orders] list in place, so [myPurchases]/[mySales]/[myRentals]/[myLeases] and every
     * screen that reads them recompose automatically. Call after login/session-restore
     * succeeds; a logged-out call is a no-op.
     */
    suspend fun refreshFromRemote() {
        val uid = currentUserId() ?: return
        try {
            val rows = supabase.from("orders").select().decodeList<OrderRow>()
            if (rows.isEmpty()) {
                _orders.clear()
                return
            }
            val counterpartyIds = rows.map { if (it.buyerId == uid) it.sellerId else it.buyerId }.distinct()
            val profiles = try {
                supabase.from("profiles").select {
                    filter { isIn("id", counterpartyIds) }
                }.decodeList<ProfileRow>().associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }
            val fresh = rows
                .filterNot { row -> if (row.buyerId == uid) row.hiddenByBuyer else row.hiddenBySeller }
                .map { row ->
                    val counterpartyId = if (row.buyerId == uid) row.sellerId else row.buyerId
                    row.toOrder(currentUserId = uid, counterpartyName = profiles[counterpartyId]?.username.orEmpty())
                }
            _orders.clear()
            _orders.addAll(fresh)
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
            android.util.Log.e("OrderRepository", "refreshFromRemote failed", e)
        }
    }

    /** Inserts a BUY order directly, then reflects it locally on success — [paymentId] is the already-verified Stripe PaymentIntent id. */
    suspend fun placeBuyOrder(product: Product, paymentId: String, checkout: CheckoutDetails, totalAmount: Double): BuyOrder? {
        val uid = currentUserId() ?: return null
        return try {
            val order = BuyOrder(
                id = UUID.randomUUID().toString(),
                ownerId = uid,
                productId = product.id,
                productTitle = product.title,
                productImage = product.images.firstOrNull().orEmpty(),
                counterpartyName = product.sellerName,
                status = OrderStatus.PAID,
                createdDate = Instant.now().toString(),
                paymentId = paymentId,
                paymentStatus = PaymentRecordStatus.PAID,
                checkout = checkout,
                price = totalAmount,
                isPurchase = true
            )
            supabase.from("orders").insert(order.toOrderRow(buyerId = uid, sellerId = product.sellerId))
            _orders.add(0, order)
            order
        } catch (e: Exception) {
            null
        }
    }

    /** Inserts a RENT order directly, then reflects it locally on success — [paymentId] is the already-verified Stripe PaymentIntent id. */
    suspend fun placeRentalOrder(
        product: Product,
        startDateMillis: Long,
        endDateMillis: Long,
        days: Int,
        dailyRate: Double,
        deposit: Double,
        totalAmount: Double,
        paymentId: String,
        checkout: CheckoutDetails
    ): RentalOrder? {
        val uid = currentUserId() ?: return null
        return try {
            val order = RentalOrder(
                id = UUID.randomUUID().toString(),
                ownerId = uid,
                productId = product.id,
                productTitle = product.title,
                productImage = product.images.firstOrNull().orEmpty(),
                counterpartyName = product.sellerName,
                status = OrderStatus.PAID,
                createdDate = Instant.now().toString(),
                paymentId = paymentId,
                paymentStatus = PaymentRecordStatus.PAID,
                checkout = checkout,
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                days = days,
                dailyRate = dailyRate,
                deposit = deposit,
                totalAmount = totalAmount,
                isRenter = true
            )
            supabase.from("orders").insert(order.toOrderRow(buyerId = uid, sellerId = product.sellerId))
            _orders.add(0, order)
            order
        } catch (e: Exception) {
            null
        }
    }

    @Serializable
    private data class AdvanceOrderStatusParams(
        @SerialName("p_order_id") val orderId: String,
        @SerialName("p_new_status") val newStatus: String
    )

    /**
     * Progresses an order to [newStatus] via the `advance_order_status` Postgres function —
     * never a direct table UPDATE. The function itself validates that this transition is legal
     * for the caller's role (buyer/seller) and the order's current status, so an invalid attempt
     * fails server-side even if this list of allowed actions ever drifts from the SQL's own copy.
     */
    suspend fun advanceStatus(order: Order, newStatus: OrderStatus): Boolean {
        val uid = currentUserId() ?: return false
        return try {
            val row = supabase.postgrest.rpc(
                "advance_order_status",
                AdvanceOrderStatusParams(orderId = order.id, newStatus = newStatus.name)
            ).decodeSingle<OrderRow>()
            val updated = row.toOrder(currentUserId = uid, counterpartyName = order.counterpartyName)
            val index = _orders.indexOfFirst { it.id == order.id }
            if (index >= 0) _orders[index] = updated
            true
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "advanceStatus(${order.id} -> $newStatus) failed", e)
            // The RPC call itself may have thrown after the update already committed server-side
            // (e.g. decoding its response failed) — refetch and check the real status before
            // reporting failure, so a genuinely-successful transition doesn't surface as an error.
            refreshFromRemote()
            _orders.any { it.id == order.id && it.status == newStatus }
        }
    }

    @Serializable
    private data class MarkOrderShippedParams(
        @SerialName("p_order_id") val orderId: String,
        @SerialName("p_courier") val courier: String?,
        @SerialName("p_tracking_number") val trackingNumber: String?
    )

    /**
     * Marks a physical handover leg (BUY outbound, RENT outbound, RENT return leg, or BUY
     * return/refund leg) as shipped via the `mark_order_shipped` Postgres function, which both
     * validates the transition and — for a SHIPPING leg — stores the courier/tracking number.
     * [courier]/[trackingNumber] are null for a MEETUP leg.
     */
    suspend fun markShipped(order: Order, courier: com.example.gadgetmover.util.Courier?, trackingNumber: String?): Boolean {
        val uid = currentUserId() ?: return false
        return try {
            val row = supabase.postgrest.rpc(
                "mark_order_shipped",
                MarkOrderShippedParams(orderId = order.id, courier = courier?.label, trackingNumber = trackingNumber)
            ).decodeSingle<OrderRow>()
            val updated = row.toOrder(currentUserId = uid, counterpartyName = order.counterpartyName)
            val index = _orders.indexOfFirst { it.id == order.id }
            if (index >= 0) _orders[index] = updated
            true
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "markShipped(${order.id}) failed", e)
            // The RPC call itself may have thrown after the update already committed server-side
            // (e.g. decoding its response failed) — refetch and check whether the order's status
            // actually moved on before reporting failure, so a genuinely-successful transition
            // doesn't surface as an error (mirrors advanceStatus's catch-and-verify above; unlike
            // that one, markShipped doesn't know its exact target status up front since the RPC
            // derives it server-side from role/order type/current status, so "status changed at
            // all from what it was before this call" is the signal instead).
            refreshFromRemote()
            _orders.any { it.id == order.id && it.status != order.status }
        }
    }

    @Serializable
    private data class HideOrderParams(@SerialName("p_order_id") val orderId: String)

    /** "Deletes" [order] from the current user's own My Activities list only — via `hide_order_for_current_user`, never a raw row DELETE, since the same row is shared with the other party (see schema.sql). */
    suspend fun hideForCurrentUser(order: Order): Boolean {
        return try {
            supabase.postgrest.rpc("hide_order_for_current_user", HideOrderParams(orderId = order.id))
            _orders.removeAll { it.id == order.id }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun myPurchases(): List<BuyOrder> =
        _orders.filterIsInstance<BuyOrder>().filter { it.isPurchase && it.ownerId == currentUserId() }
    fun mySales(): List<BuyOrder> =
        _orders.filterIsInstance<BuyOrder>().filter { !it.isPurchase && it.ownerId ==  currentUserId() }
    fun myRentals(): List<RentalOrder> =
        _orders.filterIsInstance<RentalOrder>().filter { it.isRenter && it.ownerId == currentUserId() }
    fun myLeases(): List<RentalOrder> =
        _orders.filterIsInstance<RentalOrder>().filter { !it.isRenter && it.ownerId == currentUserId() }
}

/**
 * Threads and messages are both backed by a single `messages` table (no separate "threads"
 * table) — a thread is just every message row between the same two users, grouped client-side by
 * [threadKey] rather than persisted as its own row (one thread per user pair, regardless of which
 * product(s) they've discussed — the thread's product header just reflects whichever product the
 * most recent message referenced). A thread only becomes visible to the other participant once a
 * first message actually exists; until then [findOrCreateThreadForProduct] hands back a
 * local-only placeholder. Deleting a thread ([deleteThread]) hides it from the deleting user's own
 * inbox via [chat_hidden_threads] rather than removing any message rows — the other participant's
 * view is unaffected, and the thread reappears if the counterparty sends a new message later.
 */
object ChatRepository {

    private val _threads = mutableStateListOf<ChatThread>()
    val threads: List<ChatThread> get() = _threads

    private val _messages = mutableStateListOf<Message>()

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    /** The client-side thread id for a (user, counterparty) pair — also used by [NotificationRepository] to link a "new message" notification back to its thread. */
    fun threadKey(userA: String, userB: String): String {
        val (lo, hi) = if (userA < userB) userA to userB else userB to userA
        return "$lo:$hi"
    }

    fun getThread(threadId: String): ChatThread? = _threads.find { it.id == threadId }

    /** Rebuilds a thread-list preview string from a raw row — `content` is empty for non-TEXT types, so the friendly preview has to be re-derived rather than read straight off the row. */
    private fun previewFor(row: MessageRow): String = when (row.messageType) {
        "IMAGE" -> "📷 Photo"
        "LOCATION" -> "📍 Location"
        "PRODUCT" -> "🏷️ ${row.metadata?.productTitle.orEmpty()}"
        "OFFER" -> "💰 Special price: ${row.metadata?.productTitle.orEmpty()}"
        else -> row.content
    }

    fun getMessages(threadId: String): List<Message> = _messages.filter { it.threadId == threadId }

    /**
     * Fetches every message the logged-in user has sent or received — RLS already scopes the
     * `messages` table to rows where they're the sender or the receiver — groups them into
     * threads (one per counterparty+product pair), and replaces the observed [threads]/message
     * cache in place, same pattern as [ProductRepository.refreshFromRemote].
     */
    suspend fun refreshFromRemote() {
        val uid = currentUserId() ?: return
        try {
            val allRows = supabase.from("messages").select().decodeList<MessageRow>().sortedBy { it.createdAt }
            val hiddenBefore = try {
                supabase.from("chat_hidden_threads").select { filter { eq("user_id", uid) } }
                    .decodeList<HiddenThreadRow>().associate { it.counterpartyId to it.hiddenBefore }
            } catch (e: Exception) {
                emptyMap()
            }
            // A deleted thread is hidden up to the moment it was deleted — older rows stay
            // excluded from this user's own view, but a new message from the counterparty after
            // that point makes the thread reappear.
            val rows = allRows.filter { row ->
                val counterpartyId = if (row.senderId == uid) row.receiverId else row.senderId
                val cutoff = hiddenBefore[counterpartyId]
                cutoff == null || row.createdAt > cutoff
            }
            _messages.clear()
            _messages.addAll(rows.map { it.toMessage(currentUserId = uid, threadId = threadKey(uid, if (it.senderId == uid) it.receiverId else it.senderId)) })

            val counterpartyIds = rows.map { if (it.senderId == uid) it.receiverId else it.senderId }.distinct()
            val profiles = try {
                supabase.from("profiles").select { filter { isIn("id", counterpartyIds) } }.decodeList<ProfileRow>().associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }
            val productIds = rows.mapNotNull { it.productId }.distinct()
            val products = if (productIds.isEmpty()) emptyMap() else try {
                supabase.from("products").select { filter { isIn("id", productIds) } }.decodeList<ProductRow>().associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }

            val freshThreads = rows.groupBy { threadKey(uid, if (it.senderId == uid) it.receiverId else it.senderId) }
                .map { (key, threadRows) ->
                    val last = threadRows.last()
                    val counterpartyId = if (last.senderId == uid) last.receiverId else last.senderId
                    val profile = profiles[counterpartyId]
                    val product = last.productId?.let { products[it] }
                    ChatThread(
                        id = key,
                        participantId = counterpartyId,
                        participantName = profile?.username.orEmpty(),
                        participantAvatar = profile?.avatarUrl.orEmpty(),
                        productId = last.productId,
                        productTitle = product?.title,
                        productImage = product?.imageUrls?.firstOrNull(),
                        lastMessage = previewFor(last),
                        lastMessageTime = last.createdAt,
                        lastMessageType = runCatching { MessageType.valueOf(last.messageType) }.getOrDefault(MessageType.TEXT),
                        unreadCount = threadRows.count { !it.isRead && it.receiverId == uid }
                    )
                }
                .sortedByDescending { it.lastMessageTime }
            // Preserve local-only placeholders from `findOrCreateThreadForProduct` (no message
            // sent yet, so no row exists to rebuild them from) — otherwise a freshly opened chat
            // gets wiped out by this refresh before the user sends anything.
            val freshIds = freshThreads.map { it.id }.toSet()
            val placeholders = _threads.filter { it.id !in freshIds && it.lastMessage.isEmpty() }
            _threads.clear()
            _threads.addAll(freshThreads)
            _threads.addAll(placeholders)
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    /**
     * Returns the existing thread with this seller (one per user pair, regardless of product), or
     * a local placeholder if they've never messaged before — no row is written until a message is
     * actually sent. An existing thread's product header is updated to [product] so a later "Rent
     * it" tap from a *different* listing than what the thread last discussed doesn't keep showing
     * stale product context or tag the next message with the wrong product.
     */
    suspend fun findOrCreateThreadForProduct(product: Product): ChatThread {
        val uid = currentUserId().orEmpty()
        val key = threadKey(uid, product.sellerId)
        val existingIndex = _threads.indexOfFirst { it.id == key }
        if (existingIndex >= 0) {
            val updated = _threads[existingIndex].copy(
                productId = product.id,
                productTitle = product.title,
                productImage = product.images.firstOrNull()
            )
            _threads[existingIndex] = updated
            return updated
        }
        val profile = try {
            supabase.from("profiles").select { filter { eq("id", product.sellerId) } }.decodeSingleOrNull<ProfileRow>()
        } catch (e: Exception) {
            null
        }
        val thread = ChatThread(
            id = key,
            participantId = product.sellerId,
            participantName = profile?.username ?: product.sellerName,
            participantAvatar = profile?.avatarUrl.orEmpty(),
            productId = product.id,
            productTitle = product.title,
            productImage = product.images.firstOrNull(),
            lastMessage = "",
            lastMessageTime = "",
            unreadCount = 0
        )
        _threads.add(0, thread)
        return thread
    }

    /** Hides this thread from the current user's own inbox (see class doc) — the counterparty's view is untouched. */
    suspend fun deleteThread(thread: ChatThread): Boolean {
        val uid = currentUserId() ?: return false
        return try {
            supabase.from("chat_hidden_threads").upsert(
                HiddenThreadRow(userId = uid, counterpartyId = thread.participantId, hiddenBefore = Instant.now().toString())
            )
            _threads.removeAll { it.id == thread.id }
            _messages.removeAll { it.threadId == thread.id }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun insertMessage(
        thread: ChatThread,
        content: String,
        type: MessageType,
        metadata: MessageMetadata?,
        previewText: String
    ): Boolean {
        val uid = currentUserId() ?: return false
        val row = MessageRow(
            id = UUID.randomUUID().toString(),
            senderId = uid,
            receiverId = thread.participantId,
            productId = thread.productId,
            content = content,
            messageType = type.name,
            metadata = metadata
        )
        return try {
            supabase.from("messages").insert(row)
            _messages.add(row.toMessage(currentUserId = uid, threadId = thread.id))
            val index = _threads.indexOfFirst { it.id == thread.id }
            if (index >= 0) {
                _threads[index] = _threads[index].copy(
                    lastMessage = previewText,
                    lastMessageTime = "Just now",
                    lastMessageType = type
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendMessage(thread: ChatThread, text: String): Boolean =
        insertMessage(thread, text, MessageType.TEXT, null, text)

    suspend fun sendImageMessage(thread: ChatThread, uri: Uri, contentResolver: ContentResolver): Boolean {
        val uid = currentUserId() ?: return false
        val url = uploadChatImage(uid, uri, contentResolver) ?: return false
        return insertMessage(thread, "", MessageType.IMAGE, MessageMetadata(imageUrl = url), "📷 Photo")
    }

    suspend fun sendLocationMessage(thread: ChatThread, latitude: Double, longitude: Double, address: String): Boolean =
        insertMessage(
            thread,
            "",
            MessageType.LOCATION,
            MessageMetadata(latitude = latitude, longitude = longitude, locationAddress = address),
            "📍 Location"
        )

    suspend fun sendProductMessage(thread: ChatThread, product: Product): Boolean =
        insertMessage(
            thread,
            "",
            MessageType.PRODUCT,
            MessageMetadata(productId = product.id, productTitle = product.title, productImage = product.images.firstOrNull()),
            "🏷️ ${product.title}"
        )

    suspend fun sendOfferMessage(thread: ChatThread, product: Product, offerSalePrice: Double?, offerRentalRate: Double?): Boolean =
        insertMessage(
            thread,
            "",
            MessageType.OFFER,
            MessageMetadata(
                productId = product.id,
                productTitle = product.title,
                productImage = product.images.firstOrNull(),
                offerSalePrice = offerSalePrice,
                offerRentalRate = offerRentalRate
            ),
            "💰 Special price: ${product.title}"
        )

    private const val CHAT_IMAGES_BUCKET = "chat-images"

    /** Same read-bytes-then-upload-then-publicUrl pattern as [ProductRepository.uploadProductImages]; returns null on failure so the caller can just skip sending rather than throw. */
    private suspend fun uploadChatImage(senderId: String, uri: Uri, contentResolver: ContentResolver): String? {
        return try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val bucket = supabase.storage.from(CHAT_IMAGES_BUCKET)
            val path = "$senderId/${UUID.randomUUID()}.jpg"
            bucket.upload(path, bytes) { upsert = true }
            bucket.publicUrl(path)
        } catch (e: Exception) {
            null
        }
    }

    /** Marks every unread message from [thread]'s counterpart as read — call when the thread is opened. */
    suspend fun markThreadRead(thread: ChatThread) {
        val uid = currentUserId() ?: return
        if (thread.unreadCount == 0) return
        try {
            supabase.from("messages").update({
                set("is_read", true)
            }) {
                filter {
                    eq("receiver_id", uid)
                    eq("sender_id", thread.participantId)
                    val productId = thread.productId
                    if (productId != null) eq("product_id", productId) else exact("product_id", null)
                }
            }
            val index = _threads.indexOfFirst { it.id == thread.id }
            if (index >= 0) _threads[index] = _threads[index].copy(unreadCount = 0)
        } catch (e: Exception) {
            // Best-effort — the unread badge just won't clear until the next refresh.
        }
    }

    val totalUnread: Int get() = _threads.sumOf { it.unreadCount }
}

/**
 * Rows are created server-side by triggers on `messages`/`orders` inserts (see schema.sql's
 * `notify_on_new_message`/`notify_on_new_order`) — this repository only reads the logged-in
 * user's own rows (RLS-scoped) and flips `is_read`, it never inserts a notification itself.
 */
object NotificationRepository {

    private val _notifications = mutableStateListOf<Notification>()
    val notifications: List<Notification> get() = _notifications

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    val unreadCount: Int get() = _notifications.count { !it.isRead }

    /** Short labels for the most recent notifications, e.g. for an inbox summary row. */
    fun recentSummary(limit: Int = 3): String =
        _notifications.take(limit).joinToString(" · ") { it.type.label }

    /** Fetches every notification for the logged-in user and replaces the observed [notifications] list in place. */
    suspend fun refreshFromRemote() {
        val uid = currentUserId() ?: return
        try {
            val rows = supabase.from("notifications").select().decodeList<NotificationRow>().sortedByDescending { it.createdAt }
            val fresh = rows.map { row ->
                val threadId = row.relatedSenderId?.let { ChatRepository.threadKey(uid, it) }
                row.toNotification().copy(relatedThreadId = threadId)
            }
            _notifications.clear()
            _notifications.addAll(fresh)
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    suspend fun markAsRead(id: String) {
        val index = _notifications.indexOfFirst { it.id == id }
        if (index < 0 || _notifications[index].isRead) return
        _notifications[index] = _notifications[index].copy(isRead = true)
        try {
            supabase.from("notifications").update({ set("is_read", true) }) {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            // Best-effort — will reconcile on the next refresh.
        }
    }

    suspend fun markAllRead() {
        val unreadIds = _notifications.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return
        for (i in _notifications.indices) {
            if (!_notifications[i].isRead) _notifications[i] = _notifications[i].copy(isRead = true)
        }
        try {
            supabase.from("notifications").update({ set("is_read", true) }) {
                filter { isIn("id", unreadIds) }
            }
        } catch (e: Exception) {
            // Best-effort — will reconcile on the next refresh.
        }
    }

    private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null

    /**
     * Subscribes to new rows inserted into `public.notifications` for [userId] via Supabase
     * Realtime, invoking [onNewNotification] for each one — the caller (see
     * RequestStartupPermissions'/NavGraph's use of this) is what actually posts an Android system
     * tray notification for it via SystemNotifier, keeping this repository free of any Android
     * Context. Only live while the app process is running: this can't wake the app from being
     * fully killed, unlike a real push service (Firebase Cloud Messaging) would. Safe to call
     * repeatedly — a second call for the same or a different user replaces the previous
     * subscription instead of stacking another one. `suspend` (not fire-and-forget) so it can
     * actually wait for [stopRealtimeListening] to finish tearing down any previous subscription
     * before opening a new one — see that function's doc for why that matters.
     */
    suspend fun startRealtimeListening(userId: String, onNewNotification: (Notification) -> Unit) {
        stopRealtimeListening()
        val channel = supabase.channel("notifications-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "notifications"
            filter("user_id", FilterOperator.EQ, userId)
        }
        realtimeChannel = channel
        realtimeJob = realtimeScope.launch {
            launch {
                changeFlow.collect { insert ->
                    val row = insert.decodeRecord<NotificationRow>()
                    val threadId = row.relatedSenderId?.let { ChatRepository.threadKey(userId, it) }
                    val notification = row.toNotification().copy(relatedThreadId = threadId)
                    if (_notifications.none { it.id == notification.id }) {
                        _notifications.add(0, notification)
                    }
                    onNewNotification(notification)
                }
            }
            try {
                channel.subscribe()
            } catch (e: Exception) {
                // Realtime unreachable (offline, project misconfigured) — refreshFromRemote()
                // (called on every Notifications-screen visit) still catches up eventually.
            }
        }
    }

    /**
     * Tears down the subscription started by [startRealtimeListening] — call on logout, or
     * before re-subscribing for the same/a different user. `suspend`, and specifically calls
     * [io.github.jan.supabase.realtime.Realtime.removeChannel] rather than the channel's own
     * fire-and-forget `unsubscribe()`: `removeChannel` is what actually drops the channel out of
     * the SDK's internal by-topic registry, which `supabase.channel("notifications-$userId")`
     * looks up by that same topic string. Without waiting for that removal, a same-user restart
     * (e.g. NavGraph's `LaunchedEffect(currentUserId)` re-running after the OS recreates the
     * Activity — vivo/OriginOS in particular is aggressive about killing backgrounded ones) could
     * get back the *old*, still-SUBSCRIBED channel object instead of a fresh one, and this SDK
     * throws `IllegalStateException: You cannot call postgresChangeFlow after joining the
     * channel` the moment [startRealtimeListening] tries to register a listener on it again.
     */
    suspend fun stopRealtimeListening() {
        realtimeJob?.cancel()
        realtimeJob = null
        val channel = realtimeChannel ?: return
        realtimeChannel = null
        try {
            supabase.realtime.removeChannel(channel)
        } catch (e: Exception) {
            // Already gone (connection dropped) — nothing to clean up.
        }
    }
}

object ReviewRepository {
    private val _reviews = mutableStateListOf<Review>()
    val reviews: List<Review> get() = _reviews
    val averageRating: Float get() = if (_reviews.isEmpty()) 0f else _reviews.map { it.rating }.average().toFloat()

    @Serializable
    private data class ReviewRow(
        val id: String,
        @SerialName("order_id") val orderId: String,
        @SerialName("reviewer_id") val reviewerId: String,
        @SerialName("seller_id") val sellerId: String,
        val rating: Int,
        val comment: String = "",
        @SerialName("created_at") val createdAt: String = "",
        @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
        @SerialName("seller_reply") val sellerReply: String? = null,
        @SerialName("seller_replied_at") val sellerRepliedAt: String? = null
    )

    /** Every review left for [sellerId], newest first, with the reviewer's display name/avatar joined in from `profiles`. */
    suspend fun refreshForSeller(sellerId: String) {
        try {
            val rows = supabase.from("reviews").select {
                filter { eq("seller_id", sellerId) }
                order("created_at", PostgrestOrder.DESCENDING)
            }.decodeList<ReviewRow>()
            val reviewerIds = rows.map { it.reviewerId }.distinct()
            val profiles = try {
                supabase.from("profiles").select { filter { isIn("id", reviewerIds) } }.decodeList<ProfileRow>().associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }
            _reviews.clear()
            _reviews.addAll(rows.map { row ->
                val profile = profiles[row.reviewerId]
                Review(
                    id = row.id,
                    reviewerName = profile?.username.orEmpty(),
                    reviewerAvatar = profile?.avatarUrl.orEmpty(),
                    rating = row.rating.toFloat(),
                    comment = row.comment,
                    date = row.createdAt,
                    imageUrls = row.imageUrls,
                    sellerReply = row.sellerReply,
                    sellerRepliedAt = row.sellerRepliedAt
                )
            })
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    @Serializable
    private data class SubmitReviewParams(
        @SerialName("p_order_id") val orderId: String,
        @SerialName("p_rating") val rating: Int,
        @SerialName("p_comment") val comment: String,
        @SerialName("p_image_urls") val imageUrls: List<String>
    )

    /** Calls `submit_review` — validated server-side (caller must be the order's buyer, order must be COMPLETED, one review per order) rather than a raw insert. */
    suspend fun submitReview(orderId: String, rating: Int, comment: String, imageUrls: List<String> = emptyList()): Boolean {
        return try {
            supabase.postgrest.rpc(
                "submit_review",
                SubmitReviewParams(orderId = orderId, rating = rating, comment = comment, imageUrls = imageUrls)
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private const val REVIEW_PHOTOS_BUCKET = "review-photos"

    /** Uploads the buyer's review photos to the public `review-photos` bucket, at `{reviewerId}/{orderId}/{index}.jpg` (matches the storage.foldername(name))[1] = auth.uid() check in schema.sql's upload policy). Same skip-on-failure behavior as [ReturnRequestRepository.uploadPhotos] — a photo that fails to upload just doesn't make it into the review rather than blocking the whole submission. */
    suspend fun uploadPhotos(orderId: String, reviewerId: String, uris: List<Uri>, contentResolver: ContentResolver): List<String> {
        val bucket = supabase.storage.from(REVIEW_PHOTOS_BUCKET)
        val urls = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEachIndexed
                val path = "$reviewerId/$orderId/$index.jpg"
                bucket.upload(path, bytes) { upsert = true }
                urls.add(bucket.publicUrl(path))
            } catch (e: Exception) {
                // Skip this photo — the rest of the review still submits.
            }
        }
        return urls
    }

    /** Whether [orderId] already has a review — gates the "Leave a Review" action on Order Detail. */
    suspend fun hasReviewed(orderId: String): Boolean {
        return try {
            supabase.from("reviews").select { filter { eq("order_id", orderId) } }.decodeList<ReviewRow>().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    @Serializable
    private data class ReplyToReviewParams(
        @SerialName("p_review_id") val reviewId: String,
        @SerialName("p_reply") val reply: String
    )

    /** Calls `reply_to_review` — validated server-side (caller must be the reviewed seller). Callable again to edit an existing reply. */
    suspend fun replyToReview(reviewId: String, reply: String): Boolean {
        return try {
            supabase.postgrest.rpc("reply_to_review", ReplyToReviewParams(reviewId = reviewId, reply = reply))
            true
        } catch (e: Exception) {
            false
        }
    }
}

/** The buyer-initiated return/refund dispute workflow — BUY orders only (see model/ReturnRequest.kt). */
object ReturnRequestRepository {

    /** Every request filed against [orderId] (up to 2, oldest first) — the most recent is the currently-relevant one. */
    suspend fun getForOrder(orderId: String): List<ReturnRequest> {
        return try {
            supabase.from("return_requests").select {
                filter { eq("order_id", orderId) }
                order("created_at", PostgrestOrder.ASCENDING)
            }.decodeList<ReturnRequestRow>().map { it.toReturnRequest() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val RETURN_PHOTOS_BUCKET = "return-request-photos"

    /** Uploads return/refund request photos to the `return-request-photos` bucket. Unlike
     * [ProductRepository.uploadProductImages]'s public bucket, this one is private — RLS-gated to
     * just the order's buyer/seller (see schema.sql) — so a plain [io.github.jan.supabase.storage.BucketApi.publicUrl]
     * 403s here; a signed URL carries its own access token, so [coil3.compose.AsyncImage] can load
     * it with a bare GET. Signed for 10 years, effectively permanent for a dispute record. */
    suspend fun uploadPhotos(orderId: String, uris: List<Uri>, contentResolver: ContentResolver): List<String> {
        val bucket = supabase.storage.from(RETURN_PHOTOS_BUCKET)
        val urls = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEachIndexed
                val path = "$orderId/$index.jpg"
                bucket.upload(path, bytes) { upsert = true }
                urls.add(bucket.createSignedUrl(path, 3650.days))
            } catch (e: Exception) {
                // Skip this photo — the rest of the request still submits.
            }
        }
        return urls
    }

    @Serializable
    private data class SubmitReturnRequestParams(
        @SerialName("p_order_id") val orderId: String,
        @SerialName("p_type") val type: String,
        @SerialName("p_reason_code") val reasonCode: String,
        @SerialName("p_reason_other") val reasonOther: String,
        @SerialName("p_refund_amount") val refundAmount: Double?,
        @SerialName("p_return_methods") val returnMethods: List<String>,
        @SerialName("p_description") val description: String,
        @SerialName("p_photo_urls") val photoUrls: List<String>,
        @SerialName("p_meetup_locations") val meetupLocations: List<MeetupLocation>
    )

    /** Calls `submit_return_request` — validated server-side (buyer only, order must be SHIPPED, max 2 attempts). Returns an error message on failure so the UI can show why (e.g. "already 2 attempts"). */
    suspend fun submitRequest(
        orderId: String,
        type: ReturnRequestType,
        reasonCode: String,
        reasonOther: String,
        refundAmount: Double?,
        returnMethods: Set<ReturnMethod>,
        description: String,
        photoUrls: List<String>,
        meetupLocations: List<MeetupLocation>
    ): Result<Unit> {
        return try {
            supabase.postgrest.rpc(
                "submit_return_request",
                SubmitReturnRequestParams(
                    orderId = orderId,
                    type = type.name,
                    reasonCode = reasonCode,
                    reasonOther = reasonOther,
                    refundAmount = refundAmount,
                    returnMethods = returnMethods.map { it.name },
                    description = description,
                    photoUrls = photoUrls,
                    meetupLocations = meetupLocations
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    private data class DecideReturnRequestParams(
        @SerialName("p_request_id") val requestId: String,
        @SerialName("p_accept") val accept: Boolean,
        @SerialName("p_rejection_reason") val rejectionReason: String?,
        @SerialName("p_final_return_method") val finalReturnMethod: String? = null,
        @SerialName("p_final_meetup_location") val finalMeetupLocation: MeetupLocation? = null,
        @SerialName("p_final_return_receiver_name") val finalReturnReceiverName: String? = null,
        @SerialName("p_final_return_phone_number") val finalReturnPhoneNumber: String? = null,
        @SerialName("p_final_return_full_address") val finalReturnFullAddress: String? = null
    )

    /** Calls `decide_return_request` — validated server-side (seller only, request must be PENDING); accepting/rejecting also advances the parent order's status and, when applicable, releases the refund. On accepting a RETURN, the seller's chosen [finalReturnMethod]/location or address land in the order's `checkout_details`, powering the existing "Ship Return"/"Confirm Return Received" flow. */
    suspend fun decide(
        requestId: String,
        accept: Boolean,
        rejectionReason: String?,
        finalReturnMethod: ReturnMethod? = null,
        finalMeetupLocation: MeetupLocation? = null,
        finalReturnAddress: Address? = null
    ): Boolean {
        return try {
            supabase.postgrest.rpc(
                "decide_return_request",
                DecideReturnRequestParams(
                    requestId = requestId,
                    accept = accept,
                    rejectionReason = rejectionReason,
                    finalReturnMethod = finalReturnMethod?.name,
                    finalMeetupLocation = finalMeetupLocation,
                    finalReturnReceiverName = finalReturnAddress?.receiverName,
                    finalReturnPhoneNumber = finalReturnAddress?.phoneNumber,
                    finalReturnFullAddress = finalReturnAddress?.fullAddress
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Each account's balance and history — backed by the per-user, RLS-scoped
 * `profiles.wallet_balance` column and `wallet_transactions` table, refreshed on
 * login/session-restore ([refreshFromRemote]) and reset on [clear] (called from
 * [AuthRepository.logout]) so no state leaks from one account into the next.
 */
object WalletRepository {

    val balance = mutableStateOf(0.0)
    private val _transactions = mutableStateListOf<WalletTransaction>()
    val transactions: List<WalletTransaction> get() = _transactions

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    suspend fun refreshFromRemote() {
        val userId = currentUserId() ?: return
        try {
            // Read fresh from `profiles`, not the possibly-stale cached AuthRepository.currentUser —
            // this is what a server-side credit_wallet()/debit_wallet() call actually changed.
            val freshBalance = supabase.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingle<ProfileRow>().walletBalance
            balance.value = freshBalance
            AuthRepository.currentUser.value = AuthRepository.currentUser.value?.copy(walletBalance = freshBalance)
            val rows = supabase.from("wallet_transactions").select {
                filter { eq("user_id", userId) }
            }.decodeList<WalletTransactionRow>()
            _transactions.clear()
            _transactions.addAll(rows.map { it.toWalletTransaction() })
        } catch (e: Exception) {
            // Keep whatever is cached if offline
        }
    }

    fun clear() {
        balance.value = 0.0
        _transactions.clear()
    }

    @Serializable
    private data class TopUpIntentRequest(
        val amount: Long,
        val currency: String = "myr",
        @SerialName("customer_id") val customerId: String? = null
    )

    @Serializable
    data class TopUpIntentInfo(
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("payment_intent_id") val paymentIntentId: String
    )

    /** [amountCents] is RM × 100, matching [CheckoutRepository.createPaymentIntent]'s convention. [customerId], when non-null, attaches the charge to that Stripe Customer with `setup_future_usage` (same as checkout) so PaymentSheet can offer the buyer's already-saved cards here too — see wallet-topup-intent Edge Function. */
    suspend fun createTopUpIntent(amountCents: Long, customerId: String? = null): Result<TopUpIntentInfo> = runCatching {
        supabase.functions.invoke("wallet-topup-intent", body = TopUpIntentRequest(amount = amountCents, customerId = customerId)).body<TopUpIntentInfo>()
    }.onFailure { android.util.Log.e("WalletRepository", "createTopUpIntent($amountCents) failed", it) }

    @Serializable
    private data class ConfirmTopUpRequest(@SerialName("payment_intent_id") val paymentIntentId: String)

    /**
     * Step 2 of "Add Funds", called right after PaymentSheet reports success: asks the
     * wallet-topup-confirm Edge Function to re-verify the charge with Stripe directly and, only
     * if that checks out, credit the balance server-side via `credit_wallet()` — this repository
     * (and every other client call) has no ability to credit a balance on its own, only to debit
     * its own (see [debit]), so a client reporting "the card charge succeeded" is never enough by
     * itself to add funds.
     */
    suspend fun confirmTopUp(paymentIntentId: String): Result<Unit> = runCatching {
        supabase.functions.invoke("wallet-topup-confirm", body = ConfirmTopUpRequest(paymentIntentId))
        refreshFromRemote()
    }.onFailure { android.util.Log.e("WalletRepository", "confirmTopUp($paymentIntentId) failed", it) }

    /**
     * Spends from the caller's own wallet balance — a [WalletTransactionType.PURCHASE] (paying
     * for an order with wallet funds, see [CheckoutViewModel]) or a
     * [WalletTransactionType.WITHDRAWAL] (see WalletWithdrawDestinationScreen). Goes through the
     * `debit_wallet` RPC rather than a direct table write so the balance/insufficient-funds check
     * happens atomically server-side, not against a possibly-stale locally cached [balance].
     */
    suspend fun debit(type: WalletTransactionType, amount: Double, description: String): Boolean {
        if (amount <= 0) return false
        if (currentUserId() == null) return false
        return try {
            supabase.postgrest.rpc(
                "debit_wallet",
                DebitWalletParams(type = type.name, amount = amount, description = description)
            )
            refreshFromRemote()
            true
        } catch (e: Exception) {
            android.util.Log.e("WalletRepository", "debit($type, $amount) failed", e)
            false
        }
    }

    @Serializable
    private data class DebitWalletParams(
        @SerialName("p_type") val type: String,
        @SerialName("p_amount") val amount: Double,
        @SerialName("p_description") val description: String
    )
}

/** The logged-in user's shipping/receiving addresses — real Supabase-backed, RLS-scoped to `auth.uid() = user_id` like every other per-user table in this project. Feeds both the Shipping Addresses profile screen and Checkout's address picker. */
object AddressRepository {

    private val _addresses = mutableStateListOf<Address>()
    val addresses: List<Address> get() = _addresses

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    suspend fun refreshFromRemote() {
        val uid = currentUserId() ?: return
        try {
            val rows = supabase.from("addresses").select {
                filter { eq("user_id", uid) }
            }.decodeList<AddressRow>()
            _addresses.clear()
            _addresses.addAll(rows.map { it.toAddress() }.sortedByDescending { it.isDefault })
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    suspend fun add(
        label: String,
        receiverName: String,
        phoneNumber: String,
        fullAddress: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Boolean {
        val uid = currentUserId() ?: return false
        val makeDefault = _addresses.isEmpty()
        val address = Address(UUID.randomUUID().toString(), label, receiverName, phoneNumber, fullAddress, latitude, longitude, makeDefault)
        return try {
            supabase.from("addresses").insert(address.toAddressRow(userId = uid))
            _addresses.add(0, address)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun update(address: Address): Boolean {
        val uid = currentUserId() ?: return false
        return try {
            supabase.from("addresses").update({
                set("label", address.label)
                set("receiver_name", address.receiverName)
                set("phone_number", address.phoneNumber)
                set("full_address", address.fullAddress)
                set("latitude", address.latitude)
                set("longitude", address.longitude)
            }) {
                filter { eq("id", address.id); eq("user_id", uid) }
            }
            val index = _addresses.indexOfFirst { it.id == address.id }
            if (index >= 0) _addresses[index] = address.copy(isDefault = _addresses[index].isDefault)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun remove(id: String): Boolean {
        val uid = currentUserId() ?: return false
        val wasDefault = _addresses.find { it.id == id }?.isDefault == true
        return try {
            supabase.from("addresses").delete { filter { eq("id", id); eq("user_id", uid) } }
            _addresses.removeAll { it.id == id }
            if (wasDefault && _addresses.isNotEmpty()) {
                setDefault(_addresses[0].id)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setDefault(id: String): Boolean {
        val uid = currentUserId() ?: return false
        return try {
            supabase.from("addresses").update({ set("is_default", true) }) {
                filter { eq("id", id); eq("user_id", uid) }
            }
            supabase.from("addresses").update({ set("is_default", false) }) {
                filter { eq("user_id", uid); neq("id", id) }
            }
            for (i in _addresses.indices) {
                _addresses[i] = _addresses[i].copy(isDefault = _addresses[i].id == id)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

object BrowseHistoryRepository {

    private val _viewedIds = mutableStateListOf<String>()
    val viewedIds: List<String> get() = _viewedIds

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    @Serializable
    private data class BrowseHistoryRow(
        @SerialName("user_id") val userId: String,
        @SerialName("product_id") val productId: String
    )

    @Serializable
    private data class BrowseHistoryProductIdRow(@SerialName("product_id") val productId: String)

    /** Loads the current user's 50 most recently viewed products, newest first — call after login/session-restore. */
    suspend fun refreshFromRemote() {
        val uid = currentUserId() ?: return
        try {
            val rows = supabase.from("browse_history").select {
                filter { eq("user_id", uid) }
                order("viewed_at", PostgrestOrder.DESCENDING)
                limit(50)
            }.decodeList<BrowseHistoryProductIdRow>()
            _viewedIds.clear()
            _viewedIds.addAll(rows.map { it.productId })
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    /** Upserts (rather than duplicates) so a repeat view just bumps `viewed_at` — the primary key is (user_id, product_id). */
    suspend fun recordView(productId: String) {
        val uid = currentUserId() ?: return
        _viewedIds.remove(productId)
        _viewedIds.add(0, productId)
        if (_viewedIds.size > 50) _viewedIds.removeAt(_viewedIds.lastIndex)
        try {
            supabase.from("browse_history").upsert(BrowseHistoryRow(userId = uid, productId = productId))
        } catch (e: Exception) {
            // Best-effort — will reconcile on the next refresh.
        }
    }

    /** Excludes the viewer's own listings even if they're still sitting in [_viewedIds] from before viewing your own product stopped being recorded — a defensive filter, not just a write-time one. */
    fun recentProducts(): List<Product> =
        _viewedIds.mapNotNull { ProductRepository.getById(it) }.filter { it.sellerId != currentUserId() }

    suspend fun clear() {
        val uid = currentUserId() ?: return
        _viewedIds.clear()
        try {
            supabase.from("browse_history").delete { filter { eq("user_id", uid) } }
        } catch (e: Exception) {
            // Best-effort — will reconcile on the next refresh.
        }
    }
}

object SettingsRepository {
    val notificationsEnabled = mutableStateOf(true)
    val marketingEmailsEnabled = mutableStateOf(true)

    /** null = follow system theme, true = force dark, false = force light — device-local, see [ThemePreferences]. */
    val forceDarkMode = mutableStateOf<Boolean?>(null)

    private fun currentUserId(): String? = AuthRepository.currentUser.value?.id

    @Serializable
    private data class SettingsRow(
        @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
        @SerialName("marketing_emails_enabled") val marketingEmailsEnabled: Boolean = true
    )

    /** Account-level toggles — synced through `profiles` so they're server-checkable, unlike [forceDarkMode]. Call after login/session-restore. */
    suspend fun refreshFromRemote() {
        val uid = currentUserId() ?: return
        try {
            val row = supabase.from("profiles").select { filter { eq("id", uid) } }.decodeSingle<SettingsRow>()
            notificationsEnabled.value = row.notificationsEnabled
            marketingEmailsEnabled.value = row.marketingEmailsEnabled
        } catch (e: Exception) {
            // Offline or RLS misconfigured — keep whatever is already loaded.
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
        val uid = currentUserId() ?: return
        try {
            supabase.from("profiles").update({ set("notifications_enabled", enabled) }) { filter { eq("id", uid) } }
        } catch (e: Exception) {
            // Best-effort — will reconcile on the next refresh.
        }
    }

    suspend fun setMarketingEmailsEnabled(enabled: Boolean) {
        marketingEmailsEnabled.value = enabled
        val uid = currentUserId() ?: return
        try {
            supabase.from("profiles").update({ set("marketing_emails_enabled", enabled) }) { filter { eq("id", uid) } }
        } catch (e: Exception) {
            // Best-effort — will reconcile on the next refresh.
        }
    }
}

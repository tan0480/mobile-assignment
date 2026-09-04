package com.example.gadgetmover.data

import com.example.gadgetmover.model.Address
import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.CheckoutDetails
import com.example.gadgetmover.model.Condition
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.Message
import com.example.gadgetmover.model.MessageMetadata
import com.example.gadgetmover.model.MessageType
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.model.Notification
import com.example.gadgetmover.model.NotificationType
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.OrderStatus
import com.example.gadgetmover.model.PaymentRecordStatus
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.ProductStatus
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.model.ReturnMethod
import com.example.gadgetmover.model.ReturnRequest
import com.example.gadgetmover.model.ReturnRequestStatus
import com.example.gadgetmover.model.ReturnRequestType
import com.example.gadgetmover.model.User
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.WalletTransaction
import com.example.gadgetmover.model.WalletTransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Wire shape of the `profiles` table. Deliberately separate from [User] (the app's UI-facing
 * domain model) because the two diverge: `profiles` never carries a password — Supabase Auth
 * owns credentials in the private `auth.users` table — and every column here maps 1:1 to a
 * real Postgres column rather than a mock display value.
 */
@Serializable
data class ProfileRow(
    val id: String,
    val username: String = "",
    @SerialName("user_id") val userId: String = "",
    val email: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    val location: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val rating: Float = 0f,
    @SerialName("rating_count") val ratingCount: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("wallet_balance") val walletBalance: Double = 0.0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("has_password") val hasPassword: Boolean = true
)

fun ProfileRow.toUser(): User = User(
    id = id,
    name = username,
    userId = userId,
    email = email,
    phone = phoneNumber,
    avatarUrl = avatarUrl,
    rating = rating,
    ratingCount = ratingCount,
    location = location,
    joinedDate = createdAt,
    isVerified = isVerified,
    walletBalance = walletBalance,
    password = "",
    hasPassword = hasPassword
)

/**
 * Wire shape of the `products` table. `specs` is the one column that isn't flat — every other
 * field maps 1:1 to a Postgres column, but the category-specific attributes (see
 * `CategoryFilterRegistry`) vary too much per category (a phone's SoC vs. a PSU's wattage) to
 * each get their own column, so they round-trip as a single `jsonb` column instead via
 * [CategoryFilterState]'s own `@Serializable` shape. `brand` stays a flat column too — cheap to
 * eyeball in the Supabase dashboard — populated from `specs`' own `brand` field. Enum fields are
 * stored/read as their Kotlin enum `.name` (e.g. "KEYBOARD"). Seller display info
 * (`sellerName`/`sellerRating` on [Product]) isn't stored on this table at all — it's resolved
 * separately from `profiles` and merged in by [ProductRepository.refreshFromRemote].
 */
@Serializable
data class ProductRow(
    val id: String,
    @SerialName("seller_id") val sellerId: String,
    val title: String,
    val description: String = "",
    val brand: String = "",
    val category: String,
    val condition: String,
    @SerialName("listing_type") val listingType: String,
    @SerialName("buy_price") val buyPrice: Double? = null,
    @SerialName("rental_price_per_day") val rentalPricePerDay: Double? = null,
    val deposit: Double? = null,
    val specs: CategoryFilterState = CategoryFilterState(),
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val location: String = "",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("has_warranty") val hasWarranty: Boolean = false,
    @SerialName("warranty_details") val warrantyDetails: String? = null,
    @SerialName("fulfillment_methods") val fulfillmentMethods: List<String> = emptyList(),
    @SerialName("meetup_locations") val meetupLocations: List<MeetupLocation> = emptyList(),
    @SerialName("standard_shipping_fee") val standardShippingFee: Double? = null,
    @SerialName("express_shipping_fee") val expressShippingFee: Double? = null,
    val status: String = "AVAILABLE",
    @SerialName("return_receiver_name") val returnReceiverName: String? = null,
    @SerialName("return_phone_number") val returnPhoneNumber: String? = null,
    @SerialName("return_full_address") val returnFullAddress: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    name?.let { raw -> enumValues<T>().find { it.name == raw } } ?: default

fun ProductRow.toProduct(sellerName: String, sellerRating: Float, isSellerVerified: Boolean = false, sellerAvatarUrl: String = ""): Product = Product(
    id = id,
    title = title,
    description = description,
    category = enumOrDefault(category, ProductCategory.ACCESSORY),
    listingType = enumOrDefault(listingType, ListingType.BUY),
    price = buyPrice,
    rentalRatePerDay = rentalPricePerDay,
    deposit = deposit,
    condition = enumOrDefault(condition, Condition.GOOD),
    specs = specs,
    images = imageUrls,
    sellerId = sellerId,
    sellerName = sellerName,
    sellerRating = sellerRating,
    sellerAvatarUrl = sellerAvatarUrl,
    location = location,
    postedDate = createdAt,
    isFeatured = isFeatured,
    hasWarranty = hasWarranty,
    warrantyDetails = warrantyDetails,
    isSellerVerified = isSellerVerified,
    fulfillmentMethods = fulfillmentMethods.mapNotNull { name -> enumValues<FulfillmentMethod>().find { it.name == name } }.toSet(),
    meetupLocations = meetupLocations,
    standardShippingFee = standardShippingFee,
    expressShippingFee = expressShippingFee,
    status = enumOrDefault(status, ProductStatus.AVAILABLE),
    returnReceiverName = returnReceiverName,
    returnPhoneNumber = returnPhoneNumber,
    returnFullAddress = returnFullAddress
)

fun Product.toProductRow(): ProductRow = ProductRow(
    id = id,
    sellerId = sellerId,
    title = title,
    description = description,
    brand = brandLabel(),
    category = category.name,
    condition = condition.name,
    listingType = listingType.name,
    buyPrice = price,
    rentalPricePerDay = rentalRatePerDay,
    deposit = deposit,
    specs = specs,
    imageUrls = images,
    location = location,
    isFeatured = isFeatured,
    hasWarranty = hasWarranty,
    warrantyDetails = warrantyDetails,
    fulfillmentMethods = fulfillmentMethods.map { it.name },
    meetupLocations = meetupLocations,
    standardShippingFee = standardShippingFee,
    expressShippingFee = expressShippingFee,
    status = status.name,
    returnReceiverName = returnReceiverName,
    returnPhoneNumber = returnPhoneNumber,
    returnFullAddress = returnFullAddress
)

/**
 * Wire shape of the `orders` table: one row per transaction, with both `buyer_id` and
 * `seller_id` on the same row (unlike the old mock repository's mirrored-row-per-side
 * pattern). Which side the logged-in user is on — and therefore whether a row belongs in
 * "my purchases" vs "my sales", or "my rentals" vs "my leases" — is derived when mapping to
 * the domain [Order] rather than stored.
 */
@Serializable
data class OrderRow(
    val id: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_title") val productTitle: String = "",
    @SerialName("product_image") val productImage: String = "",
    @SerialName("order_type") val orderType: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("deposit_amount") val depositAmount: Double? = null,
    @SerialName("rental_daily_rate") val rentalDailyRate: Double? = null,
    @SerialName("rental_days") val rentalDays: Int? = null,
    @SerialName("rental_start_date") val rentalStartDate: String? = null,
    @SerialName("rental_end_date") val rentalEndDate: String? = null,
    val status: String = "COMPLETED",
    @SerialName("payment_id") val paymentId: String? = null,
    @SerialName("payment_status") val paymentStatus: String = "PENDING",
    @SerialName("checkout_details") val checkoutDetails: CheckoutDetails = CheckoutDetails(),
    @SerialName("hidden_by_buyer") val hiddenByBuyer: Boolean = false,
    @SerialName("hidden_by_seller") val hiddenBySeller: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

private fun dateStringToMillis(date: String?): Long {
    if (date.isNullOrBlank()) return 0L
    return try {
        LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}

private fun millisToDateString(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

/** [currentUserId] decides which side of the transaction this row represents for "my ___" filtering. */
fun OrderRow.toOrder(currentUserId: String, counterpartyName: String): Order {
    val isMineAsBuyer = buyerId == currentUserId
    val status = enumOrDefault(status, OrderStatus.COMPLETED)
    val paymentRecordStatus = enumOrDefault(paymentStatus, PaymentRecordStatus.PENDING)
    return if (orderType == "RENT") {
        RentalOrder(
            id = id,
            ownerId = currentUserId,
            productId = productId.orEmpty(),
            productTitle = productTitle,
            productImage = productImage,
            counterpartyName = counterpartyName,
            status = status,
            createdDate = createdAt,
            paymentId = paymentId,
            paymentStatus = paymentRecordStatus,
            checkout = checkoutDetails,
            startDateMillis = dateStringToMillis(rentalStartDate),
            endDateMillis = dateStringToMillis(rentalEndDate),
            days = rentalDays ?: 0,
            dailyRate = rentalDailyRate ?: 0.0,
            deposit = depositAmount ?: 0.0,
            totalAmount = totalAmount,
            isRenter = isMineAsBuyer
        )
    } else {
        BuyOrder(
            id = id,
            ownerId = currentUserId,
            productId = productId.orEmpty(),
            productTitle = productTitle,
            productImage = productImage,
            counterpartyName = counterpartyName,
            status = status,
            createdDate = createdAt,
            paymentId = paymentId,
            paymentStatus = paymentRecordStatus,
            checkout = checkoutDetails,
            price = totalAmount,
            isPurchase = isMineAsBuyer
        )
    }
}

fun BuyOrder.toOrderRow(buyerId: String, sellerId: String): OrderRow = OrderRow(
    id = id,
    buyerId = buyerId,
    sellerId = sellerId,
    productId = productId,
    productTitle = productTitle,
    productImage = productImage,
    orderType = "BUY",
    totalAmount = price,
    status = status.name,
    paymentId = paymentId,
    paymentStatus = paymentStatus.name,
    checkoutDetails = checkout
)

fun RentalOrder.toOrderRow(buyerId: String, sellerId: String): OrderRow = OrderRow(
    id = id,
    buyerId = buyerId,
    sellerId = sellerId,
    productId = productId,
    productTitle = productTitle,
    productImage = productImage,
    orderType = "RENT",
    totalAmount = totalAmount,
    depositAmount = deposit,
    rentalDailyRate = dailyRate,
    rentalDays = days,
    rentalStartDate = millisToDateString(startDateMillis),
    rentalEndDate = millisToDateString(endDateMillis),
    status = status.name,
    paymentId = paymentId,
    paymentStatus = paymentStatus.name,
    checkoutDetails = checkout
)

/**
 * Wire shape of the `messages` table. There is no separate "threads" table — a conversation
 * thread is just every row between the same two users, grouped client-side (see
 * [ChatRepository]'s thread-key helper) rather than persisted as its own row.
 */
@Serializable
data class MessageRow(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("product_id") val productId: String? = null,
    val content: String,
    @SerialName("message_type") val messageType: String = "TEXT",
    val metadata: MessageMetadata? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

/** Wire shape of `chat_hidden_threads` — one row per (user, counterparty) they've deleted the conversation with; see [ChatRepository.deleteThread]. */
@Serializable
data class HiddenThreadRow(
    @SerialName("user_id") val userId: String,
    @SerialName("counterparty_id") val counterpartyId: String,
    @SerialName("hidden_before") val hiddenBefore: String
)

fun MessageRow.toMessage(currentUserId: String, threadId: String): Message = Message(
    id = id,
    threadId = threadId,
    senderId = senderId,
    text = content,
    timestamp = createdAt,
    isFromMe = senderId == currentUserId,
    type = runCatching { MessageType.valueOf(messageType) }.getOrDefault(MessageType.TEXT),
    metadata = metadata
)

/**
 * Wire shape of the `notifications` table. The app never inserts rows here itself — they're
 * created server-side by triggers on `messages`/`orders` inserts (see schema.sql) — it only
 * reads its own and flips `is_read`. [relatedSenderId]/[relatedProductId] let the client
 * reconstruct the chat thread a "new message" notification points at (via
 * [ChatRepository.threadKey]) without the DB needing to know about that client-only concept.
 */
@Serializable
data class NotificationRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val message: String = "",
    @SerialName("related_product_id") val relatedProductId: String? = null,
    @SerialName("related_sender_id") val relatedSenderId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

fun NotificationRow.toNotification(): Notification = Notification(
    id = id,
    type = enumOrDefault(type, NotificationType.LISTING_UPDATE),
    title = title,
    message = message,
    timestamp = createdAt,
    isRead = isRead
)

/** Wire shape of the `addresses` table — one row per saved shipping/receiving address, RLS-scoped to its owner. */
@Serializable
data class AddressRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val label: String = "",
    @SerialName("receiver_name") val receiverName: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("full_address") val fullAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

fun AddressRow.toAddress(): Address = Address(
    id = id,
    label = label,
    receiverName = receiverName,
    phoneNumber = phoneNumber,
    fullAddress = fullAddress,
    latitude = latitude,
    longitude = longitude,
    isDefault = isDefault
)

fun Address.toAddressRow(userId: String): AddressRow = AddressRow(
    id = id,
    userId = userId,
    label = label,
    receiverName = receiverName,
    phoneNumber = phoneNumber,
    fullAddress = fullAddress,
    latitude = latitude,
    longitude = longitude,
    isDefault = isDefault
)

/** Wire shape of the `wallet_transactions` table — the per-user, RLS-scoped audit trail behind [ProfileRow.walletBalance]. */
@Serializable
data class WalletTransactionRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val amount: Double,
    val description: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

fun WalletTransactionRow.toWalletTransaction(): WalletTransaction = WalletTransaction(
    id = id,
    type = enumOrDefault(type, WalletTransactionType.DEPOSIT),
    amount = amount,
    description = description,
    date = createdAt
)

/** Wire shape of the `return_requests` table. */
@Serializable
data class ReturnRequestRow(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("attempt_number") val attemptNumber: Int,
    @SerialName("request_type") val requestType: String,
    @SerialName("reason_code") val reasonCode: String,
    @SerialName("reason_other_text") val reasonOtherText: String = "",
    @SerialName("refund_amount") val refundAmount: Double? = null,
    @SerialName("return_method") val returnMethod: String? = null,
    @SerialName("meetup_location") val meetupLocation: MeetupLocation? = null,
    val description: String = "",
    @SerialName("photo_urls") val photoUrls: List<String> = emptyList(),
    val status: String = "PENDING",
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

fun ReturnRequestRow.toReturnRequest(): ReturnRequest = ReturnRequest(
    id = id,
    orderId = orderId,
    requesterId = requesterId,
    attemptNumber = attemptNumber,
    requestType = enumOrDefault(requestType, ReturnRequestType.RETURN),
    reasonCode = reasonCode,
    reasonOtherText = reasonOtherText,
    refundAmount = refundAmount,
    returnMethod = returnMethod?.let { enumOrDefault(it, ReturnMethod.MEETUP) },
    meetupLocation = meetupLocation,
    description = description,
    photoUrls = photoUrls,
    status = enumOrDefault(status, ReturnRequestStatus.PENDING),
    rejectionReason = rejectionReason,
    createdDate = createdAt
)

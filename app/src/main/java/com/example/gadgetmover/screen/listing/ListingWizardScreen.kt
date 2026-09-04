package com.example.gadgetmover.screen.listing

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AddressRepository
import com.example.gadgetmover.data.ListingDraftRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.data.SavedListingDraft
import com.example.gadgetmover.model.Condition
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.screen.components.AddPhotoTile
import com.example.gadgetmover.screen.components.NameMeetupLocationDialog
import com.example.gadgetmover.screen.components.PickedLocation
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.FilterField
import com.example.gadgetmover.model.filter.isFilled
import com.example.gadgetmover.model.filter.isValidForListing
import com.example.gadgetmover.model.filter.isVisible
import com.example.gadgetmover.screen.checkout.ShippingTier
import com.example.gadgetmover.screen.explore.filter.DynamicFilterField
import com.example.gadgetmover.util.parseListingNumber
import com.example.gadgetmover.util.resolveSellerLocation
import com.example.gadgetmover.util.sanitizeMoneyInput
import com.example.gadgetmover.util.validateListingNumbers

import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.ListingCompletenessScore
import com.example.gadgetmover.util.ListingScoreCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Lets [ListingDraft.imageUris] — local content-picker URIs, needed only for the save-as-draft JSON — round-trip through kotlinx.serialization as plain strings. */
private object UriSerializer : KSerializer<Uri> {
    override val descriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Uri) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Uri = Uri.parse(decoder.decodeString())
}

/** Not `private` — [com.example.gadgetmover.data.ListingDraftRepository] persists this directly as the body of a saved draft. */
@Serializable
internal data class ListingDraft(
    val listingType: ListingType = ListingType.BUY,
    val category: ProductCategory? = null,
    val title: String = "",
    val description: String = "",
    val condition: Condition = Condition.GOOD,
    /** The category's schema-driven fields — see `CategoryFilterRegistry` — as the seller fills them in on the Specifications step. Exactly the shape stored on [Product.specs]. */
    val categorySpecs: CategoryFilterState = CategoryFilterState(),
    val price: String = "",
    val rentalRate: String = "",
    val deposit: String = "",
    val hasWarranty: Boolean = false,
    val warrantyDetails: String = "",
    val imageUris: List<@Serializable(with = UriSerializer::class) Uri> = emptyList(),
    /** Already-uploaded photo URLs kept from the listing being edited — distinct from [imageUris], which are newly-picked local photos still awaiting upload. Empty when creating a brand-new listing. */
    val existingImageUrls: List<String> = emptyList(),
    val fulfillmentMethods: Set<FulfillmentMethod> = emptySet(),
    val meetupLocations: List<MeetupLocation> = emptyList(),
    /** Only meaningful when [FulfillmentMethod.SHIPPING] is in [fulfillmentMethods] — the seller sets what each speed actually costs to ship this item, rather than a platform-fixed rate. */
    val standardShippingFee: String = "",
    val expressShippingFee: String = "",
    /** Only used to highlight the selected radio row in [StepPricing]'s return-address picker — [returnAddressSnapshot] is the actual value saved. */
    val returnAddressId: String? = null,
    /**
     * Only meaningful for a RENT/BOTH listing with SHIPPING enabled — the seller's own address,
     * captured as a snapshot the moment they pick it (or, in the Edit flow, seeded from the
     * listing's already-saved snapshot even if that address can no longer be matched back to a
     * live entry in [com.example.gadgetmover.data.AddressRepository]), so an edit never silently
     * loses a previously-set return address just because [returnAddressId] failed to re-resolve.
     */
    val returnAddressSnapshot: ReturnAddressSnapshot? = null
) {
    /** Whether this draft is worth prompting to save / showing in the draft picker — an empty step-0 draft is not. */
    fun isWorthSaving(): Boolean = title.isNotBlank() || category != null || imageUris.isNotEmpty() || existingImageUrls.isNotEmpty() || price.isNotBlank() || rentalRate.isNotBlank()
}

@Serializable
internal data class ReturnAddressSnapshot(val receiverName: String, val phoneNumber: String, val fullAddress: String)

private data class ListingWizardUiState(
    val step: Int = 0,
    val draft: ListingDraft = ListingDraft(),
    val isPublishing: Boolean = false,
    val pendingMeetupPick: PickedLocation? = null,
    /** Identifies this in-progress draft in [ListingDraftRepository] — stable for the life of this ViewModel so repeated saves (explicit or autosave) overwrite the same entry instead of piling up duplicates. */
    val draftId: String = UUID.randomUUID().toString(),
    /** The [draft] content as of the last successful save (explicit, autosave, or "Save Draft" from the leave prompt) — null means never saved. As long as [draft] still equals this, there's nothing new to lose, so the bottom-nav "leave without saving?" prompt stays quiet. */
    val lastSavedDraft: ListingDraft? = null
)

/** Seeds a draft from an existing listing so the Edit flow reuses every step's UI as-is — 1:1 field mapping, numeric fields become their text-input string form. */
private fun draftFrom(product: Product): ListingDraft = ListingDraft(
    listingType = product.listingType,
    category = product.category,
    title = product.title,
    description = product.description,
    condition = product.condition,
    categorySpecs = product.specs,
    price = product.price?.toString().orEmpty(),
    rentalRate = product.rentalRatePerDay?.toString().orEmpty(),
    deposit = product.deposit?.toString().orEmpty(),
    hasWarranty = product.hasWarranty,
    warrantyDetails = product.warrantyDetails.orEmpty(),
    existingImageUrls = product.images,
    fulfillmentMethods = product.fulfillmentMethods,
    meetupLocations = product.meetupLocations,
    standardShippingFee = product.standardShippingFee?.toString().orEmpty(),
    expressShippingFee = product.expressShippingFee?.toString().orEmpty(),
    // Best-effort reverse-match against the seller's current address book, purely to highlight
    // the right radio row — stays null if that address was since edited/deleted, but
    // returnAddressSnapshot below is seeded from the product regardless, so the save path never
    // loses it just because the id lookup failed.
    returnAddressId = AddressRepository.addresses.find {
        it.fullAddress == product.returnFullAddress && it.receiverName == product.returnReceiverName
    }?.id,
    returnAddressSnapshot = product.returnFullAddress?.let { address ->
        ReturnAddressSnapshot(
            receiverName = product.returnReceiverName.orEmpty(),
            phoneNumber = product.returnPhoneNumber.orEmpty(),
            fullAddress = address
        )
    }
)

/**
 * Holds the in-progress draft in a ViewModel — scoped to this screen's own NavBackStackEntry —
 * purely so it survives navigating to the Location Picker and back. Plain `remember` state does
 * not: Navigation-Compose fully disposes a destination's composition while a child destination
 * (the picker) is on top of it, and only a ViewModelStore (tied to the back stack entry, not the
 * composition) is kept alive across that round trip.
 *
 * [existingProduct] non-null means this is the Edit flow (the draft is seeded from it and
 * [publish] updates that same listing in place); null means creating a brand-new listing.
 */
private class ListingWizardViewModel(private val existingProduct: Product?) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ListingWizardUiState(draft = existingProduct?.let(::draftFrom) ?: ListingDraft())
    )
    val uiState: StateFlow<ListingWizardUiState> = _uiState

    fun goToStep(step: Int) {
        _uiState.update { it.copy(step = step) }
    }

    fun updateDraft(draft: ListingDraft) {
        _uiState.update { it.copy(draft = draft) }
    }

    /** Resumes a previously saved draft picked from the draft picker — replaces the in-progress draft and jumps back to whichever step it was saved at. */
    fun loadFromSavedDraft(saved: SavedListingDraft) {
        _uiState.update { it.copy(draft = saved.draft, draftId = saved.id, step = saved.step, lastSavedDraft = saved.draft) }
    }

    /** Records that [draft] was just persisted to [ListingDraftRepository] — see [ListingWizardUiState.lastSavedDraft]. */
    fun markDraftSaved(draft: ListingDraft) {
        _uiState.update { it.copy(lastSavedDraft = draft) }
    }

    fun setPendingMeetupPick(pick: PickedLocation?) {
        _uiState.update { it.copy(pendingMeetupPick = pick) }
    }

    fun publish(contentResolver: ContentResolver, onResult: suspend (Boolean) -> Unit) {
        _uiState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            val product = existingProduct
            val success = if (product != null) {
                updateListing(product, _uiState.value.draft, contentResolver)
            } else {
                publishListing(_uiState.value.draft, contentResolver)
            }
            _uiState.update { it.copy(isPublishing = false) }
            onResult(success)
        }
    }
}

private class ListingWizardViewModelFactory(private val existingProduct: Product?) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ListingWizardViewModel(existingProduct) as T
}

private val stepTitles = listOf(
    "Listing Type", "Category", "Item Details", "Specifications", "Photos", "Price"
)
private val stepHeadlines = listOf(
    "How will this item move?",
    "What are you listing?",
    "Tell us about your item",
    "Technical specifications",
    "Add photos",
    "Set your price"
)
private val stepSubtitles = listOf(
    "Your form will adapt to the listing type you choose.",
    "Choose the category that best fits your item.",
    "Add details buyers need to know.",
    "Help buyers find exactly what they need.",
    "Listings with photos get more views.",
    "You can adjust this anytime after publishing."
)

/**
 * Reported by [ListingWizardScreen] whenever there's something on screen worth not losing — a
 * non-empty new-listing draft, or an edit that differs from the published listing. [onSaveAndLeave]
 * performs whichever save is appropriate (an instant local draft write, or the same async publish
 * behind the "Save Changes" button) and calls its `onSaved` callback once it's safe to navigate
 * away — draft saves call it immediately, an edit's publish call only on success.
 */
internal class WizardUnsavedChanges(val onSaveAndLeave: (onSaved: () -> Unit) -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListingWizardScreen(
    existingProduct: Product?,
    onBackClick: () -> Unit,
    onPublished: () -> Unit,
    onLoginClick: () -> Unit,
    pickedMeetupLocation: PickedLocation?,
    onPickMeetupLocation: () -> Unit,
    /** Reports unsaved-changes state (null once there's nothing worth saving, e.g. while the draft picker is showing) — lets the caller guard the bottom nav bar's other tabs behind a "leave without saving?" prompt instead of silently discarding them. Covers both the new-listing draft flow and the Edit flow — see [WizardUnsavedChanges]. */
    onUnsavedChangesChanged: (WizardUnsavedChanges?) -> Unit = {}
) {
    val screenTitle = if (existingProduct != null) "Edit Listing" else "List an item"
    val isLoggedIn by AuthRepository.isLoggedIn
    val sessionRestored by AuthRepository.sessionRestored

    if (!sessionRestored) {
        // Still checking for a persisted session — showing nothing here rather than the "log in"
        // state below, which would otherwise flash for an already-logged-in user for the brief
        // moment before AuthRepository.restoreSession() resolves.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isLoggedIn) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "List an item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Log in to list an item for sale or rent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onLoginClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Log In")
                    }
                }
            }
        }
        return
    }

    val viewModel: ListingWizardViewModel = viewModel(factory = remember(existingProduct?.id) { ListingWizardViewModelFactory(existingProduct) })
    val uiState by viewModel.uiState.collectAsState()
    val step = uiState.step
    val draft = uiState.draft
    val isPublishing = uiState.isPublishing
    val pendingMeetupPick = uiState.pendingMeetupPick
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    // The Edit flow's baseline for detecting unsaved changes — what the wizard was seeded with,
    // before any edits. Only ever read for that comparison, never re-seeded into the draft itself.
    val originalEditDraft = remember(existingProduct?.id) { existingProduct?.let(::draftFrom) }

    // A fresh pick from the map (§5) triggers the "name this location" dialog below, rather than
    // being appended straight away — the picker only returns coordinates + a geocoded address.
    LaunchedEffect(pickedMeetupLocation) {
        if (pickedMeetupLocation != null) viewModel.setPendingMeetupPick(pickedMeetupLocation)
    }

    // Drafts only exist for brand-new listings — an Edit flow already has its real, published
    // starting point, so it skips straight to the wizard below.
    var showDraftPicker by remember { mutableStateOf(false) }
    var savedDrafts by remember { mutableStateOf<List<SavedListingDraft>>(emptyList()) }
    // Whether the wizard was reached by picking "Resume"/"Start New" on the draft picker — the
    // step-0 back arrow only exists (and only ever leads back to the picker) when this is true;
    // a wizard entered directly (no saved drafts to pick from) has no step-0 back arrow at all,
    // since leaving now happens through the guarded bottom nav bar instead (see
    // [onUnsavedChangesChanged]).
    var cameFromDraftPicker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (existingProduct == null) {
            val drafts = ListingDraftRepository.loadAll(context)
            if (drafts.isNotEmpty()) {
                savedDrafts = drafts
                showDraftPicker = true
            }
        }
    }

    // Reports unsaved-changes state up to the caller (see [onUnsavedChangesChanged]'s doc) — null
    // while the picker itself is showing (nothing being actively edited), once a new-listing draft
    // matches uiState.lastSavedDraft (nothing new to lose since the last save), or once an edit
    // matches [originalEditDraft] (nothing changed from the published listing).
    LaunchedEffect(showDraftPicker, uiState) {
        val isDirty = if (existingProduct != null) {
            draft != originalEditDraft
        } else {
            draft.isWorthSaving() && draft != uiState.lastSavedDraft
        }
        onUnsavedChangesChanged(
            if (showDraftPicker || !isDirty) {
                null
            } else if (existingProduct != null) {
                WizardUnsavedChanges(onSaveAndLeave = { onSaved ->
                    viewModel.publish(contentResolver) { success ->
                        if (success) {
                            onSaved()
                        } else {
                            snackbarHostState.showSnackbar("Couldn't save your changes. Please try again.")
                        }
                    }
                })
            } else {
                WizardUnsavedChanges(onSaveAndLeave = { onSaved ->
                    ListingDraftRepository.save(
                        context,
                        SavedListingDraft(id = uiState.draftId, savedAt = System.currentTimeMillis(), draft = draft, step = step)
                    )
                    viewModel.markDraftSaved(draft)
                    onSaved()
                })
            }
        )
    }

    if (showDraftPicker) {
        DraftPickerScreen(
            drafts = savedDrafts,
            onResume = { saved ->
                viewModel.loadFromSavedDraft(saved)
                cameFromDraftPicker = true
                showDraftPicker = false
            },
            onDelete = { saved ->
                ListingDraftRepository.delete(context, saved.id)
                savedDrafts = savedDrafts.filterNot { it.id == saved.id }
            },
            onStartNew = {
                cameFromDraftPicker = true
                showDraftPicker = false
            }
        )
        return
    }

    // Autosaves silently when the app is backgrounded (e.g. the user hits Home) mid-listing —
    // there's no opportunity to prompt once the app leaves the foreground, so this is the safety
    // net for that case; the explicit "Save as draft" button and the "leave without saving?"
    // prompt guarding the bottom nav bar are the interactive paths. Deliberately NOT
    // LocalLifecycleOwner.current here — inside a NavHost `composable{}` destination that resolves
    // to the NavBackStackEntry's own lifecycle, which also STOPs on ordinary in-app navigation
    // away from this screen (e.g. tapping "Leave" in that very prompt), which would silently
    // re-save the draft the user just chose to discard. The Activity's own lifecycle only stops
    // on genuine backgrounding.
    val latestUiState by rememberUpdatedState(uiState)
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && existingProduct == null && latestUiState.draft.isWorthSaving()) {
                ListingDraftRepository.save(
                    context,
                    SavedListingDraft(id = latestUiState.draftId, savedAt = System.currentTimeMillis(), draft = latestUiState.draft, step = latestUiState.step)
                )
                viewModel.markDraftSaved(latestUiState.draft)
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    var showSaveDraftConfirm by remember { mutableStateOf(false) }
    // Only shown/wired at step 0 when the wizard was reached via the draft picker — it just
    // returns there, no confirmation needed since nothing is lost (the in-progress draft stays
    // in the ViewModel). Leaving the wizard entirely is now guarded at the bottom nav bar instead.
    val showStepZeroBack = existingProduct != null || cameFromDraftPicker
    val handleBack: () -> Unit = {
        if (step == 0) {
            if (existingProduct == null && cameFromDraftPicker) showDraftPicker = true else onBackClick()
        } else {
            viewModel.goToStep(step - 1)
        }
    }
    BackHandler(enabled = step > 0 || showStepZeroBack, onBack = handleBack)

    // Hoisted out of `StepTechnicalSpecs` so the topBar (below) can pin a compact version of this
    // while the seller scrolls the specs list — otherwise it's only visible at the very top of the
    // step, defeating the point of live feedback while filling in fields further down.
    val specSchema = remember(draft.category) { draft.category?.let { CategoryFilterRegistry.schemaFor(it) } }
    val specCompleteness = specSchema?.let { ListingScoreCalculator.score(it, draft.categorySpecs) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0 || showStepZeroBack) {
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                    Text(
                        screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(44.dp))
                }
                // Shown once a category is picked and the seller has moved past the Category step
                // itself (step 1) — showing it there too would just duplicate what `StepCategory`
                // is already displaying front and center.
                if (draft.category != null && step > 1) {
                    CategoryBadgeRow(draft.category)
                }
                // Pinned (not inside the scrollable step content) so it stays visible while the
                // seller scrolls through the specs fields below — see `specCompleteness` above.
                if (step == 3 && specCompleteness != null) {
                    StickySpecCompletenessBar(specCompleteness)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                ListingProgressCard(step = step)

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "STEP ${step + 1} · ${stepTitles[step].uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stepHeadlines[step],
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stepSubtitles[step],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                when (step) {
                    0 -> StepTransactionType(draft) { viewModel.updateDraft(it) }
                    1 -> StepCategory(draft) { viewModel.updateDraft(it) }
                    2 -> StepConditionAndBasics(draft, onChange = { viewModel.updateDraft(it) }, onPickMeetupLocation = onPickMeetupLocation)
                    3 -> StepTechnicalSpecs(draft) { viewModel.updateDraft(it) }
                    4 -> StepPhotos(draft) { viewModel.updateDraft(it) }
                    5 -> StepPricing(draft) { viewModel.updateDraft(it) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingProduct == null) {
                    OutlinedButton(
                        onClick = { showSaveDraftConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save as draft")
                    }
                }
                Button(
                    onClick = {
                        if (step < 5) {
                            viewModel.goToStep(step + 1)
                        } else {
                            viewModel.publish(contentResolver) { success ->
                                if (success) {
                                    if (existingProduct == null) ListingDraftRepository.delete(context, uiState.draftId)
                                    onPublished()
                                } else {
                                    snackbarHostState.showSnackbar("Couldn't publish your listing. Please try again.")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = isStepValid(step, draft) && !isPublishing
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                    } else {
                        Text(if (step < 5) "Continue" else if (existingProduct != null) "Save Changes" else "Publish Listing")
                    }
                }
            }
        }
    }

    pendingMeetupPick?.let { pick ->
        NameMeetupLocationDialog(
            address = pick.address,
            initialName = pick.suggestedName,
            onDismiss = { viewModel.setPendingMeetupPick(null) },
            onConfirm = { name ->
                viewModel.updateDraft(
                    draft.copy(meetupLocations = draft.meetupLocations + MeetupLocation(UUID.randomUUID().toString(), name, pick.address, pick.latitude, pick.longitude))
                )
                viewModel.setPendingMeetupPick(null)
                // Best-effort, silent — lets buyers filter by state (see CommonFilterFields.sellerState)
                // without asking the seller to fill in a separate address field just for that.
                scope.launch {
                    resolveSellerLocation(context, pick.latitude, pick.longitude)?.let { resolved ->
                        AuthRepository.updateSellerLocation(resolved.city, resolved.state)
                    }
                }
            }
        )
    }

    if (showSaveDraftConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveDraftConfirm = false },
            title = { Text("Save as draft?") },
            text = { Text("You can pick up where you left off next time you come back.") },
            confirmButton = {
                Button(onClick = {
                    ListingDraftRepository.save(
                        context,
                        SavedListingDraft(id = uiState.draftId, savedAt = System.currentTimeMillis(), draft = draft, step = step)
                    )
                    viewModel.markDraftSaved(draft)
                    showSaveDraftConfirm = false
                    scope.launch { snackbarHostState.showSnackbar("Draft saved") }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDraftConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

/** Shown when entering the "List an item" flow fresh while saved drafts exist — lets the seller resume one, delete one, or start clean. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftPickerScreen(
    drafts: List<SavedListingDraft>,
    onResume: (SavedListingDraft) -> Unit,
    onDelete: (SavedListingDraft) -> Unit,
    onStartNew: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Drafts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Pick up where you left off, or start a brand-new listing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onStartNew,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start a New Listing")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(drafts, key = { it.id }) { saved ->
                DraftCard(saved = saved, onResume = { onResume(saved) }, onDelete = { onDelete(saved) })
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun DraftCard(saved: SavedListingDraft, onResume: () -> Unit, onDelete: () -> Unit) {
    val draft = saved.draft
    var confirmingDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onResume),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val thumbnail: Any? = draft.existingImageUrls.firstOrNull() ?: draft.imageUris.firstOrNull()
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    draft.title.ifBlank { draft.category?.label ?: "Untitled draft" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = listOfNotNull(
                    draft.category?.label,
                    "Saved ${formatDraftSavedAt(saved.savedAt)}"
                ).joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            IconButton(onClick = { confirmingDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete draft", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this draft?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatDraftSavedAt(millis: Long): String = SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(millis))

@Composable
private fun ListingProgressCard(step: Int) {
    val percent = ((step + 1) * 100) / stepTitles.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(18.dp)
    ) {
        Text(
            "LISTING PROGRESS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step ${step + 1} of ${stepTitles.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "$percent%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (step + 1) / stepTitles.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stepTitles.forEachIndexed { index, label ->
                StepCircle(index = index, label = label, currentStep = step)
            }
        }
    }
}

@Composable
private fun RowScope.StepCircle(index: Int, label: String, currentStep: Int) {
    val isActive = index == currentStep
    val isDone = index < currentStep
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isActive || isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (index + 1).toString(),
                color = if (isActive || isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** The one schema field every category requires — see `CategoryFilterRegistry` — before a listing can publish; every other schema field stays optional. */
private fun requiredBrandField(category: ProductCategory?): FilterField? =
    category?.let { CategoryFilterRegistry.schemaFor(it) }?.sections?.find { it.key == "brand" }

const val TITLE_CHAR_LIMIT = 100
const val DESCRIPTION_CHAR_LIMIT = 2000

private fun isStepValid(step: Int, draft: ListingDraft): Boolean = when (step) {
    1 -> draft.category != null
    2 -> draft.title.isNotBlank() &&
        draft.title.length <= TITLE_CHAR_LIMIT &&
        draft.description.length <= DESCRIPTION_CHAR_LIMIT &&
        draft.fulfillmentMethods.isNotEmpty() &&
        (FulfillmentMethod.MEETUP !in draft.fulfillmentMethods || draft.meetupLocations.isNotEmpty())
    3 -> {
        val brandFilled = requiredBrandField(draft.category)?.let { draft.categorySpecs.valueFor(it.key).isFilled(it) } ?: true
        val schema = draft.category?.let { CategoryFilterRegistry.schemaFor(it) }
        val specsInBounds = schema?.sections.orEmpty()
            .filter { it.isVisible(draft.categorySpecs) }
            .all { field -> draft.categorySpecs.valueFor(field.key).isValidForListing(field) }
        brandFilled && specsInBounds
    }
    5 -> validateListingNumbers(
        listingType = draft.listingType,
        price = draft.price,
        rentalRate = draft.rentalRate,
        deposit = draft.deposit,
        shippingEnabled = FulfillmentMethod.SHIPPING in draft.fulfillmentMethods,
        standardShippingFee = draft.standardShippingFee,
        expressShippingFee = draft.expressShippingFee
    ) && (!needsReturnAddress(draft) || draft.returnAddressSnapshot != null)
    else -> true
}

/** A RENT/BOTH listing with SHIPPING enabled needs a return address for renters to ship the item back to — see [ListingDraft.returnAddressId]. */
private fun needsReturnAddress(draft: ListingDraft): Boolean =
    (draft.listingType == ListingType.RENT || draft.listingType == ListingType.BOTH) &&
        FulfillmentMethod.SHIPPING in draft.fulfillmentMethods

private suspend fun publishListing(draft: ListingDraft, contentResolver: ContentResolver): Boolean {
    val seller = AuthRepository.currentUser.value ?: return false
    val productId = UUID.randomUUID().toString()
    val imageUrls = ProductRepository.uploadProductImages(
        sellerId = seller.id,
        productId = productId,
        uris = draft.imageUris,
        contentResolver = contentResolver
    )
    val product = Product(
        id = productId,
        title = draft.title,
        description = draft.description.ifBlank { "No description provided." },
        category = draft.category ?: ProductCategory.ACCESSORY,
        listingType = draft.listingType,
        price = parseListingNumber(draft.price),
        rentalRatePerDay = parseListingNumber(draft.rentalRate),
        deposit = parseListingNumber(draft.deposit, allowZero = true),
        condition = draft.condition,
        specs = draft.categorySpecs,
        images = imageUrls,
        sellerId = seller.id,
        sellerName = seller.name,
        sellerRating = seller.rating,
        location = seller.location,
        postedDate = "Just now",
        hasWarranty = draft.hasWarranty,
        warrantyDetails = draft.warrantyDetails.ifBlank { null },
        fulfillmentMethods = draft.fulfillmentMethods,
        meetupLocations = draft.meetupLocations,
        standardShippingFee = parseListingNumber(draft.standardShippingFee, allowZero = true),
        expressShippingFee = parseListingNumber(draft.expressShippingFee, allowZero = true),
        returnReceiverName = draft.returnAddressSnapshot?.receiverName.takeIf { needsReturnAddress(draft) },
        returnPhoneNumber = draft.returnAddressSnapshot?.phoneNumber.takeIf { needsReturnAddress(draft) },
        returnFullAddress = draft.returnAddressSnapshot?.fullAddress.takeIf { needsReturnAddress(draft) }
    )
    return ProductRepository.addProduct(product)
}

/** Updates [existingProduct] in place via `.copy(...)` — preserves id/sellerId/sellerName/etc. automatically, only overwriting the fields the wizard's steps actually edit. Only newly-picked [ListingDraft.imageUris] get uploaded; [ListingDraft.existingImageUrls] are kept as-is. */
private suspend fun updateListing(existingProduct: Product, draft: ListingDraft, contentResolver: ContentResolver): Boolean {
    val newlyUploadedUrls = ProductRepository.uploadProductImages(
        sellerId = existingProduct.sellerId,
        productId = existingProduct.id,
        uris = draft.imageUris,
        contentResolver = contentResolver
    )
    val product = existingProduct.copy(
        title = draft.title,
        description = draft.description.ifBlank { "No description provided." },
        category = draft.category ?: existingProduct.category,
        listingType = draft.listingType,
        price = parseListingNumber(draft.price),
        rentalRatePerDay = parseListingNumber(draft.rentalRate),
        deposit = parseListingNumber(draft.deposit, allowZero = true),
        condition = draft.condition,
        specs = draft.categorySpecs,
        images = draft.existingImageUrls + newlyUploadedUrls,
        hasWarranty = draft.hasWarranty,
        warrantyDetails = draft.warrantyDetails.ifBlank { null },
        fulfillmentMethods = draft.fulfillmentMethods,
        meetupLocations = draft.meetupLocations,
        standardShippingFee = parseListingNumber(draft.standardShippingFee, allowZero = true),
        expressShippingFee = parseListingNumber(draft.expressShippingFee, allowZero = true),
        returnReceiverName = draft.returnAddressSnapshot?.receiverName.takeIf { needsReturnAddress(draft) },
        returnPhoneNumber = draft.returnAddressSnapshot?.phoneNumber.takeIf { needsReturnAddress(draft) },
        returnFullAddress = draft.returnAddressSnapshot?.fullAddress.takeIf { needsReturnAddress(draft) }
    )
    return ProductRepository.updateProduct(product)
}

@Composable
private fun StepTransactionType(draft: ListingDraft, onChange: (ListingDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ListingType.entries.forEach { type ->
            SelectableRow(
                title = type.label,
                subtitle = when (type) {
                    ListingType.BUY -> "Sell it to a new owner"
                    ListingType.RENT -> "Earn while it is not in use"
                    ListingType.BOTH -> "Offer both options to buyers"
                },
                icon = when (type) {
                    ListingType.BUY -> Icons.Filled.AttachMoney
                    ListingType.RENT -> Icons.Filled.Schedule
                    ListingType.BOTH -> Icons.AutoMirrored.Filled.CompareArrows
                },
                selected = draft.listingType == type,
                onClick = { onChange(draft.copy(listingType = type)) }
            )
        }
    }
}

@Composable
private fun StepCategory(draft: ListingDraft, onChange: (ListingDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProductCategory.entries.forEach { cat ->
            SelectableRow(
                title = cat.label,
                subtitle = null,
                selected = draft.category == cat,
                onClick = { onChange(draft.copy(category = cat)) }
            )
        }
    }
}

@Composable
private fun StepConditionAndBasics(draft: ListingDraft, onChange: (ListingDraft) -> Unit, onPickMeetupLocation: () -> Unit) {
    Column {
        LabeledField("Title") {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { onChange(draft.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                placeholder = { Text("e.g. Keychron Q1 Pro Custom Build") },
                isError = draft.title.length > TITLE_CHAR_LIMIT,
                supportingText = {
                    Text(
                        if (draft.title.length > TITLE_CHAR_LIMIT) "Title cannot exceed $TITLE_CHAR_LIMIT characters"
                        else "${draft.title.length} / $TITLE_CHAR_LIMIT"
                    )
                }
            )
        }
        LabeledField("Description") {
            OutlinedTextField(
                value = draft.description,
                onValueChange = { onChange(draft.copy(description = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Describe the item's condition, usage history, and reason for selling") },
                isError = draft.description.length > DESCRIPTION_CHAR_LIMIT,
                supportingText = {
                    Text(
                        if (draft.description.length > DESCRIPTION_CHAR_LIMIT) "Description cannot exceed $DESCRIPTION_CHAR_LIMIT characters"
                        else "${draft.description.length} / $DESCRIPTION_CHAR_LIMIT"
                    )
                }
            )
        }
        Text("Condition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Condition.entries) { c ->
                ChoiceChip(
                    label = c.label,
                    selected = draft.condition == c,
                    onClick = { onChange(draft.copy(condition = c)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("How can buyers get this item?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FulfillmentMethod.entries.forEach { method ->
                ChoiceChip(
                    label = method.label,
                    selected = method in draft.fulfillmentMethods,
                    onClick = {
                        val updated = if (method in draft.fulfillmentMethods) {
                            draft.fulfillmentMethods - method
                        } else {
                            draft.fulfillmentMethods + method
                        }
                        onChange(draft.copy(fulfillmentMethods = updated))
                    }
                )
            }
        }

        if (FulfillmentMethod.MEETUP in draft.fulfillmentMethods) {
            Spacer(modifier = Modifier.height(14.dp))
            draft.meetupLocations.forEach { location ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(location.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(location.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(onClick = { onChange(draft.copy(meetupLocations = draft.meetupLocations - location)) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove location")
                        }
                    }
                }
            }
            OutlinedButton(onClick = onPickMeetupLocation, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add meet-up location")
            }
        }
    }
}

/**
 * Driven entirely by the picked category's own advanced-filter schema (see
 * [CategoryFilterRegistry]) — the exact same [DynamicFilterField] widgets the Explore screen's
 * filter sheet uses, so whatever the seller fills in here is exactly what a buyer can filter on
 * and see on the product detail page later. Only `brand` is required (checked by
 * [isStepValid]/[requiredBrandField]); every other field is optional and simply won't show up on
 * the product page if left blank.
 */
@Composable
private fun StepTechnicalSpecs(draft: ListingDraft, onChange: (ListingDraft) -> Unit) {
    val schema = draft.category?.let { CategoryFilterRegistry.schemaFor(it) }
    if (schema == null) {
        Text(
            "Pick a category first to see its specifications.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column {
        schema.sections.filter { it.isVisible(draft.categorySpecs) }.forEach { field ->
            DynamicFilterField(
                field = field,
                value = draft.categorySpecs.valueFor(field.key),
                state = draft.categorySpecs,
                onValueChange = { newValue ->
                    onChange(draft.copy(categorySpecs = draft.categorySpecs.with(field.key, newValue)))
                },
                isListing = true
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/** Compact "Category: X" row pinned in the wizard's topBar once a category is picked (see call site) — a lightweight reminder of which category's schema is driving the rest of the wizard, without repeating `StepCategory`'s own full picker UI. */
@Composable
private fun CategoryBadgeRow(category: ProductCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Category",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                category.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Live "Spec Completeness: 80%" feedback, pinned in the wizard's topBar (not scrolled away) for
 * the whole Technical Specs step — recomputed on every keystroke/selection since it just reads
 * [draft.categorySpecs] via [ListingScoreCalculator], with a tier label/color that tells the
 * seller how close they are to the full [+50% search boost][ListingCompletenessScore.boostMultiplier]
 * a fully-specced listing gets (see [screen.home.HomeScreen]/[screen.explore.ExploreScreen] ranking).
 * Deliberately compact (single-line label + thin bar, no caption) since it's competing for
 * permanent screen space with the topBar's title row and — once past the Category step — the
 * [CategoryBadgeRow] above it.
 */
@Composable
private fun StickySpecCompletenessBar(completeness: ListingCompletenessScore) {
    val percent = completeness.percent
    val (label, color) = when {
        percent > 85 -> "✨ Max Boost (+50%)" to BrandOrange
        percent >= 50 -> "Good visibility" to MaterialTheme.colorScheme.primary
        else -> "Add specs to boost" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Spec Completeness: $percent%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { completeness.completenessRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun StepPricing(draft: ListingDraft, onChange: (ListingDraft) -> Unit) {
    Column {
        if (draft.listingType == ListingType.BUY || draft.listingType == ListingType.BOTH) {
            LabeledField("Sale Price (RM)") {
                OutlinedTextField(
                    value = draft.price,
                    onValueChange = { onChange(draft.copy(price = sanitizeMoneyInput(it))) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Text("RM") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
        if (draft.listingType == ListingType.RENT || draft.listingType == ListingType.BOTH) {
            LabeledField("Rental Rate (RM / day)") {
                OutlinedTextField(
                    value = draft.rentalRate,
                    onValueChange = { onChange(draft.copy(rentalRate = sanitizeMoneyInput(it))) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Text("RM") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            LabeledField("Security Deposit (RM)") {
                OutlinedTextField(
                    value = draft.deposit,
                    onValueChange = { onChange(draft.copy(deposit = it.filter { c -> c.isDigit() || c == '.' })) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Text("RM") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
        if (FulfillmentMethod.SHIPPING in draft.fulfillmentMethods) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Shipping Fees", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Set what you'll charge the buyer for each speed you offer — leave a field blank to not offer it. Below is a reference only, not a fixed rate.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LabeledField("Standard Delivery Fee (RM)") {
                OutlinedTextField(
                    value = draft.standardShippingFee,
                    onValueChange = { onChange(draft.copy(standardShippingFee = it.filter { c -> c.isDigit() || c == '.' })) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Text("RM") },
                    placeholder = { Text("e.g. RM${ShippingTier.STANDARD.fee.toInt()}") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            LabeledField("Express Delivery Fee (RM)") {
                OutlinedTextField(
                    value = draft.expressShippingFee,
                    onValueChange = { onChange(draft.copy(expressShippingFee = it.filter { c -> c.isDigit() || c == '.' })) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Text("RM") },
                    placeholder = { Text("e.g. RM${ShippingTier.EXPRESS.fee.toInt()}") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
        if (needsReturnAddress(draft)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Return Address", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Where should renters ship this item back to when they're done?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (AddressRepository.addresses.isEmpty()) {
                Text(
                    "Add a shipping address from Profile > Shipping Addresses first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AddressRepository.addresses.forEach { address ->
                        val selected = draft.returnAddressId == address.id
                        Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(
                                            draft.copy(
                                                returnAddressId = address.id,
                                                returnAddressSnapshot = ReturnAddressSnapshot(
                                                    receiverName = address.receiverName,
                                                    phoneNumber = address.phoneNumber,
                                                    fullAddress = address.fullAddress
                                                )
                                            )
                                        )
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${address.receiverName}   ${address.phoneNumber}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(address.fullAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                }
                            }
                        }
                    }
                }
                if (draft.returnAddressId == null && draft.returnAddressSnapshot != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Currently: ${draft.returnAddressSnapshot.fullAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ToggleRow("Includes warranty", draft.hasWarranty) { onChange(draft.copy(hasWarranty = it)) }
        if (draft.hasWarranty) {
            LabeledField("Warranty Details") {
                OutlinedTextField(
                    value = draft.warrantyDetails,
                    onValueChange = { onChange(draft.copy(warrantyDetails = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("e.g. Manufacturer warranty until Dec 2026") }
                )
            }
        }
    }
}

private const val MAX_LISTING_PHOTOS = 8

@Composable
private fun StepPhotos(draft: ListingDraft, onChange: (ListingDraft) -> Unit) {
    val totalCount = draft.existingImageUrls.size + draft.imageUris.size

    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(draft.existingImageUrls) { url ->
                Box(modifier = Modifier.size(84.dp)) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { onChange(draft.copy(existingImageUrls = draft.existingImageUrls - url)) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            items(draft.imageUris) { uri ->
                Box(modifier = Modifier.size(84.dp)) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { onChange(draft.copy(imageUris = draft.imageUris - uri)) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (totalCount < MAX_LISTING_PHOTOS) {
                item {
                    AddPhotoTile(
                        maxSelectable = MAX_LISTING_PHOTOS,
                        cameraSubDir = "listing_photos",
                        onPhotosPicked = { uris -> onChange(draft.copy(imageUris = uris)) },
                        onPhotoCaptured = { uri -> onChange(draft.copy(imageUris = draft.imageUris + uri)) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "$totalCount/$MAX_LISTING_PHOTOS photos added — optional, but listings with photos get more views",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LabeledField(label: String, field: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        field()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandOrange.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BrandOrange else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrandOrange)
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BrandOrange else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

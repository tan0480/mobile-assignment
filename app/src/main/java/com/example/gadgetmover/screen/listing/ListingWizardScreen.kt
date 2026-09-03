package com.example.gadgetmover.screen.listing

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AddressRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.Condition
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.screen.components.PickedLocation
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.FilterField
import com.example.gadgetmover.model.filter.isFilled
import com.example.gadgetmover.model.filter.isVisible
import com.example.gadgetmover.screen.checkout.ShippingTier
import com.example.gadgetmover.screen.explore.filter.DynamicFilterField
import com.example.gadgetmover.util.parseListingNumber
import com.example.gadgetmover.util.sanitizeMoneyInput
import com.example.gadgetmover.util.validateListingNumbers

import com.example.gadgetmover.ui.theme.BrandOrange
import java.util.UUID

private data class ListingDraft(
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
    val imageUris: List<Uri> = emptyList(),
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
)

private data class ReturnAddressSnapshot(val receiverName: String, val phoneNumber: String, val fullAddress: String)

private data class ListingWizardUiState(
    val step: Int = 0,
    val draft: ListingDraft = ListingDraft(),
    val isPublishing: Boolean = false,
    val pendingMeetupPick: PickedLocation? = null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingWizardScreen(
    existingProduct: Product?,
    onBackClick: () -> Unit,
    onPublished: () -> Unit,
    onLoginClick: () -> Unit,
    pickedMeetupLocation: PickedLocation?,
    onPickMeetupLocation: () -> Unit
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: swap with custom ImageVector
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
                        Icons.Filled.AddAPhoto, // TODO: swap with custom ImageVector
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
    val contentResolver = LocalContext.current.contentResolver

    // A fresh pick from the map (§5) triggers the "name this location" dialog below, rather than
    // being appended straight away — the picker only returns coordinates + a geocoded address.
    LaunchedEffect(pickedMeetupLocation) {
        if (pickedMeetupLocation != null) viewModel.setPendingMeetupPick(pickedMeetupLocation)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (step == 0) onBackClick() else viewModel.goToStep(step - 1) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: swap with custom ImageVector
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
                OutlinedButton(
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Draft saved — pick up where you left off anytime") }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save as draft")
                }
                Button(
                    onClick = {
                        if (step < 5) {
                            viewModel.goToStep(step + 1)
                        } else {
                            viewModel.publish(contentResolver) { success ->
                                if (success) {
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
            }
        )
    }
}

@Composable
private fun NameMeetupLocationDialog(address: String, initialName: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this meet-up spot") },
        text = {
            Column {
                Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("e.g. TAR UMT, KLCC LRT") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.ifBlank { "Meet-up spot" }) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

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

private fun isStepValid(step: Int, draft: ListingDraft): Boolean = when (step) {
    1 -> draft.category != null
    2 -> draft.title.isNotBlank() &&
        draft.fulfillmentMethods.isNotEmpty() &&
        (FulfillmentMethod.MEETUP !in draft.fulfillmentMethods || draft.meetupLocations.isNotEmpty())
    3 -> requiredBrandField(draft.category)?.let { draft.categorySpecs.valueFor(it.key).isFilled(it) } ?: true
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
                }, // TODO: swap with custom ImageVector
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
                placeholder = { Text("e.g. Keychron Q1 Pro Custom Build") }
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
                placeholder = { Text("Describe the item's condition, usage history, and reason for selling") }
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
                            Icon(Icons.Filled.Close, contentDescription = "Remove location") // TODO: swap with custom ImageVector
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
    val pickPhotos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_LISTING_PHOTOS)
    ) { uris -> if (uris.isNotEmpty()) onChange(draft.copy(imageUris = uris)) }

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
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo") // TODO: swap with custom ImageVector
                    }
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

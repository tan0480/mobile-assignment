package com.example.gadgetmover.screen.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ChatRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.ChatThread
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.Message
import com.example.gadgetmover.model.MessageType
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductStatus
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.screen.components.BackgroundLoadingBadge
import com.example.gadgetmover.screen.components.FullScreenImageViewer
import com.example.gadgetmover.screen.components.PickedLocation
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.formatDisplayDate
import com.example.gadgetmover.util.formatMoney
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    thread: ChatThread,
    onBackClick: () -> Unit,
    pickedLocation: PickedLocation? = null,
    onPickLocationClick: () -> Unit = {},
    onLocationConsumed: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onNegotiatedCheckout: (productId: String, transactionType: ListingType, price: Double) -> Unit = { _, _, _ -> }
) {
    // Not `remember`-wrapped — reads the snapshot-backed message cache directly so this screen
    // reactively picks up whatever `refreshFromRemote()` below (or a pull-to-refresh) just pulled.
    val messages = ChatRepository.getMessages(thread.id)
    var input by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var isBackgroundLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    // imePadding() on the content Box only shrinks the viewport when the keyboard opens — it
    // doesn't re-scroll the list, so whatever was at the bottom (e.g. this offer card's Buy
    // button) stays at the same item position but now falls outside the shrunk, keyboard-covered
    // viewport. A single animateScrollToItem fired once the keyboard starts opening isn't enough:
    // its target position is computed against the viewport size at that instant, but the IME
    // inset keeps growing every frame for the rest of the open animation, so the list falls behind
    // and ends up still partly tucked under the keyboard/input bar. Keying the effect on the raw
    // IME height (not just an open/closed boolean) re-fires it on every frame of that animation,
    // snapping (not animating — cheap, and avoids stacking animations mid-flight) the list to the
    // bottom each time so it tracks the keyboard up in lockstep and lands correctly once it settles.
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottomPx) {
        if (imeBottomPx > 0 && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    // Only a real drag from the user should dismiss the keyboard — listState.isScrollInProgress
    // would also be true for our own programmatic animateScrollToItem below (e.g. right as the
    // user sends a message), which would wrongly close the keyboard they're actively typing with.
    // interactionSource's DragInteraction.Start fires only for an actual touch-drag on the list.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }
    }

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showProductPickerFor by remember { mutableStateOf<AttachmentAction?>(null) }
    var offerProduct by remember { mutableStateOf<Product?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch { ChatRepository.sendImageMessage(thread, uri, context.contentResolver) }
        }
    }

    // A single sequential effect rather than two independent ones: returning from the location
    // picker recomposes this screen fresh (a brand-new composition, since Navigation-Compose
    // disposes the previous destination when navigating away), so pickedLocation is already
    // resolved by the time this first runs. Running the location send and the initial refresh as
    // two separate concurrent LaunchedEffects raced — whichever of the two network round trips
    // happened to resolve last won, so the refresh (fetching a snapshot from before the insert)
    // could clobber the just-sent message and it would only reappear after a manual pull-to-
    // refresh. Doing it all in one coroutine removes the race entirely.
    LaunchedEffect(Unit) {
        pickedLocation?.let {
            ChatRepository.sendLocationMessage(thread, it.latitude, it.longitude, it.address)
            onLocationConsumed()
        }
        isBackgroundLoading = true
        ChatRepository.refreshFromRemote()
        isBackgroundLoading = false
        ChatRepository.markThreadRead(thread)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    if (showAttachmentMenu) {
        AttachmentMenuSheet(
            onDismiss = { showAttachmentMenu = false },
            onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickLocation = onPickLocationClick,
            onPickProduct = { showProductPickerFor = AttachmentAction.SHARE_PRODUCT },
            onPickSpecialPrice = { showProductPickerFor = AttachmentAction.SPECIAL_PRICE }
        )
    }

    showProductPickerFor?.let { action ->
        ProductPickerSheet(
            onDismiss = { showProductPickerFor = null },
            onSelect = { product ->
                showProductPickerFor = null
                when (action) {
                    AttachmentAction.SHARE_PRODUCT -> scope.launch { ChatRepository.sendProductMessage(thread, product) }
                    AttachmentAction.SPECIAL_PRICE -> offerProduct = product
                }
            }
        )
    }

    previewImageUrl?.let { url ->
        FullScreenImageViewer(imageUrl = url, onDismiss = { previewImageUrl = null })
    }

    offerProduct?.let { product ->
        OfferPriceDialog(
            product = product,
            onDismiss = { offerProduct = null },
            onConfirm = { salePrice, rentalRate ->
                offerProduct = null
                scope.launch { ChatRepository.sendOfferMessage(thread, product, salePrice, rentalRate) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = thread.participantAvatar,
                            contentDescription = thread.participantName,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(thread.participantName, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // imePadding() belongs on this outer container, not just the floating input row below —
        // applying it here shrinks the whole content area (list + floating bar together) so the
        // list's own "scroll to last item" lands correctly above the keyboard instead of the
        // keyboard silently covering the bottom of an unshrunk list and pushing content up
        // behind the TopAppBar.
        Box(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            AppPullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        ChatRepository.refreshFromRemote()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 88.dp,
                        // The pinned product banner below is an 8.dp top offset + a 44.dp image +
                        // 10.dp vertical padding on each side = 72.dp tall — reserving only 64.dp
                        // here left its bottom 8.dp overlapping the top of the first message, so
                        // scrolling all the way up never fully cleared the banner off of it.
                        top = if (thread.productTitle != null) 80.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(messages) { _, message ->
                        ChatBubble(message, onProductClick, onNegotiatedCheckout, onImageClick = { previewImageUrl = it })
                    }
                }

                // Floats over the message list rather than taking its own full-width row, so only
                // the gray pill itself — not a full-width transparent strip around it — sits over the
                // chat; tapping it opens that product's detail page.
                if (thread.productTitle != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable(enabled = thread.productId != null) { thread.productId?.let(onProductClick) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    AsyncImage(
                        model = thread.productImage,
                        contentDescription = thread.productTitle,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        thread.productTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                }
                BackgroundLoadingBadge(visible = isBackgroundLoading, modifier = Modifier.align(Alignment.TopCenter))
            }

            // Floats over the chat like the product banner above rather than sitting in a
            // Scaffold bottomBar slot with its own full-width opaque surface — the row itself has
            // no background, so only the bordered text field and buttons are visible and the chat
            // behind/around them stays in view.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    // OutlinedTextField's container is transparent by default, which was fine
                    // sitting on the old opaque bottomBar — now that this row floats with no
                    // background of its own, an explicit solid color keeps the chat from showing
                    // through the field.
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showAttachmentMenu = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add attachment")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotBlank()) {
                            input = ""
                            scope.launch {
                                ChatRepository.sendMessage(thread, text)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrandOrange)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

private enum class AttachmentAction { SHARE_PRODUCT, SPECIAL_PRICE }

@Composable
private fun ChatBubble(
    message: Message,
    onProductClick: (String) -> Unit,
    onNegotiatedCheckout: (productId: String, transactionType: ListingType, price: Double) -> Unit,
    onImageClick: (String) -> Unit
) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) BrandOrange else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        when (message.type) {
            MessageType.TEXT -> Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(bubbleShape(message.isFromMe))
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            }

            MessageType.IMAGE -> AsyncImage(
                model = message.metadata?.imageUrl,
                contentDescription = "Photo",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = message.metadata?.imageUrl != null) {
                        message.metadata?.imageUrl?.let(onImageClick)
                    },
                contentScale = ContentScale.Crop
            )

            MessageType.LOCATION -> {
                val context = LocalContext.current
                val lat = message.metadata?.latitude
                val lng = message.metadata?.longitude
                Row(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .clip(bubbleShape(message.isFromMe))
                        .background(bubbleColor)
                        .clickable(enabled = lat != null && lng != null) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        message.metadata?.locationAddress.orEmpty(),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                }
            }

            MessageType.PRODUCT -> {
                val productId = message.metadata?.productId
                val liveProduct = productId?.let { ProductRepository.getById(it) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = productId != null) { productId?.let(onProductClick) }
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = message.metadata?.productImage,
                            contentDescription = message.metadata?.productTitle,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                message.metadata?.productTitle.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                            if (liveProduct != null) {
                                Text(
                                    when (liveProduct.listingType) {
                                        ListingType.RENT -> "${formatMoney(liveProduct.rentalRatePerDay ?: 0.0)} / day"
                                        else -> formatMoney(liveProduct.price ?: 0.0)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            MessageType.OFFER -> {
                val productId = message.metadata?.productId
                val liveProduct = productId?.let { ProductRepository.getById(it) }
                val soldOut = liveProduct == null || liveProduct.status == ProductStatus.SOLD
                // The seller is the one who sends this offer in the first place — their own copy
                // of the thread shouldn't offer a "Buy Now"/"Rent" straight into checkout for
                // their own listing, same as the product detail page already hides those for
                // the owner (see ProductDetailScreen's isOwner check).
                val isOwnListing = liveProduct?.sellerId == AuthRepository.currentUser.value?.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = productId != null) { productId?.let(onProductClick) }
                        .padding(12.dp)
                ) {
                    Text(
                        "💰 Special Price",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = message.metadata?.productImage,
                            contentDescription = message.metadata?.productTitle,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            message.metadata?.productTitle.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (soldOut) {
                        Text(
                            "No longer available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        val salePrice = message.metadata?.offerSalePrice
                        val rentalRate = message.metadata?.offerRentalRate
                        if (salePrice != null) {
                            OfferPriceRow(liveProduct.price, salePrice)
                            if (!isOwnListing) {
                                Button(
                                    onClick = { onNegotiatedCheckout(productId, ListingType.BUY, salePrice) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Buy Now — ${formatMoney(salePrice)}") }
                            }
                            if (rentalRate != null) Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (rentalRate != null) {
                            OfferPriceRow(liveProduct.rentalRatePerDay, rentalRate, suffix = "/day")
                            if (!isOwnListing) {
                                OutlinedButton(
                                    onClick = { onNegotiatedCheckout(productId, ListingType.RENT, rentalRate) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Rent — ${formatMoney(rentalRate)}/day") }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            formatDisplayDate(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OfferPriceRow(originalPrice: Double?, discountedPrice: Double, suffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        if (originalPrice != null) {
            Text(
                "${formatMoney(originalPrice)}$suffix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            "${formatMoney(discountedPrice)}$suffix",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun bubbleShape(isFromMe: Boolean) = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = if (isFromMe) 16.dp else 4.dp,
    bottomEnd = if (isFromMe) 4.dp else 16.dp
)

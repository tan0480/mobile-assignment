package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.OrderRepository
import com.example.gadgetmover.data.ReviewRepository
import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.DepositStatus
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.OrderStatus
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.model.isHolding
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.ui.theme.SuccessGreen
import com.example.gadgetmover.ui.theme.WarningAmber
import com.example.gadgetmover.util.Courier
import com.example.gadgetmover.util.estimatedPayout
import com.example.gadgetmover.util.formatDisplayDate
import com.example.gadgetmover.util.formatMoney
import com.example.gadgetmover.util.openInGoogleMaps
import com.example.gadgetmover.util.openUrl
import com.example.gadgetmover.util.tracksAutomatically
import com.example.gadgetmover.util.trackingUrl
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: Order,
    onBackClick: () -> Unit,
    onDeleted: () -> Unit,
    fromNotification: Boolean = false,
    onNotificationBack: () -> Unit = onBackClick,
    onRequestReturnClick: () -> Unit = {},
    onReviewRequestClick: () -> Unit = {},
    onWriteReviewClick: () -> Unit = {},
    onProductClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var alreadyReviewed by remember { mutableStateOf(true) }
    // Tapping the tracking number only stages this — the actual copy+open happens once the user
    // confirms in the AlertDialog below, since jumping straight to another app with no warning
    // felt too abrupt.
    var pendingTrackTap by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showReleaseDepositDialog by remember { mutableStateOf(false) }
    var isReleasingDeposit by remember { mutableStateOf(false) }
    val handleBack = if (fromNotification) onNotificationBack else onBackClick

    // A notification is an entry point, not a screen in the user's browsing history. Handle the
    // system gesture/button exactly like the app-bar arrow so neither can reveal Notifications.
    BackHandler(enabled = fromNotification) { onNotificationBack() }

    // Most couriers' official tracking pages run their search through client-side JS rather than
    // a plain URL parameter, so this always copies the number to the clipboard as a reliable
    // fallback; only Courier.NINJA_VAN's tracking URL is confirmed to pre-fill it automatically.
    fun performTrackTap(courierLabel: String, trackingNumber: String) {
        clipboard.setText(AnnotatedString(trackingNumber))
        val courier = Courier.entries.firstOrNull { it.label == courierLabel }
        val url = courier?.trackingUrl(trackingNumber)
        if (url != null) {
            openUrl(context, url)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (courier.tracksAutomatically()) "Opening $courierLabel tracking"
                    else "Tracking number copied — paste it into $courierLabel's search"
                )
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Tracking number copied") }
        }
    }

    val isBuyerSide = when (order) {
        is BuyOrder -> order.isPurchase
        is RentalOrder -> order.isRenter
    }
    val isSellerSide = !isBuyerSide
    val canReview = order.status == OrderStatus.TO_REVIEW && isBuyerSide
    val canRequestReturn = order is BuyOrder && isBuyerSide && order.status == OrderStatus.SHIPPED
    val canReviewReturnRequest = order is BuyOrder && isSellerSide && order.status == OrderStatus.RETURN_REQUESTED
    val canDelete = order.status.isDeletable
    val payoutPending = when (order) {
        is BuyOrder -> order.status == OrderStatus.SHIPPED
        is RentalOrder -> order.status in setOf(OrderStatus.RENTAL_SHIPPED, OrderStatus.RENTING, OrderStatus.RETURN_PENDING)
    }
    val rental = order as? RentalOrder
    val hasDeposit = rental?.deposit?.let { it > 0.0 } == true
    val depositHolding = rental?.let { it.deposit > 0.0 && it.checkout.depositStatus.isHolding } == true
    val depositRefunded = rental?.let { it.deposit > 0.0 && it.checkout.depositStatus == DepositStatus.REFUNDED } == true
    val canReleaseDeposit = isSellerSide && depositHolding && order.status in setOf(
        OrderStatus.TO_REVIEW,
        OrderStatus.COMPLETED,
        OrderStatus.RETURNED
    )

    LaunchedEffect(order.id) {
        if (canReview) alreadyReviewed = ReviewRepository.hasReviewed(order.id)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canDelete) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove from history")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ProductHeaderCard(order, onClick = onProductClick)
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Order Info")
            Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    InfoRow("Order ID", "#${order.id.take(8).uppercase()}")
                    InfoRow("Status", order.status.label)
                    InfoRow("Payment", order.paymentStatus.name.lowercase().replaceFirstChar { it.uppercase() })
                    InfoRow("With", order.counterpartyName)
                    InfoRow("Order Date", formatDisplayDate(order.createdDate))
                    order.shippedAt?.let { InfoRow("Shipped", formatDisplayDate(it)) }
                    order.receivedAt?.let { InfoRow("Received", formatDisplayDate(it)) }
                    order.returnShippedAt?.let { InfoRow("Return Shipped", formatDisplayDate(it)) }
                    order.returnReceivedAt?.let { InfoRow("Return Received", formatDisplayDate(it)) }
                    if (order is RentalOrder) {
                        InfoRow("Rental Period", "${formatDate(order.startDateMillis)} – ${formatDate(order.endDateMillis)}")
                        InfoRow("Duration", "${order.days} day${if (order.days == 1) "" else "s"}")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Fulfillment")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FulfillmentDetail(
                        label = "Receiving",
                        method = order.checkout.receivingMethod,
                        meetup = order.checkout.receivingMeetup,
                        receiverName = order.checkout.shippingReceiverName,
                        phoneNumber = order.checkout.shippingPhoneNumber,
                        fullAddress = order.checkout.shippingFullAddress,
                        courier = order.checkout.outboundCourier,
                        trackingNumber = order.checkout.outboundTrackingNumber,
                        onOpenMaps = { openInGoogleMaps(context, it) },
                        onTrackTap = { courierLabel, trackingNumber -> pendingTrackTap = courierLabel to trackingNumber }
                    )
                    val returningMethod = order.checkout.returningMethod
                    if (returningMethod != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        FulfillmentDetail(
                            label = "Returning",
                            method = returningMethod,
                            meetup = order.checkout.returningMeetup,
                            receiverName = order.checkout.returnReceiverName,
                            phoneNumber = order.checkout.returnPhoneNumber,
                            fullAddress = order.checkout.returnFullAddress,
                            courier = order.checkout.returnCourier,
                            trackingNumber = order.checkout.returnTrackingNumber,
                            onOpenMaps = { openInGoogleMaps(context, it) },
                            onTrackTap = { courierLabel, trackingNumber -> pendingTrackTap = courierLabel to trackingNumber }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Price Breakdown")
            PriceBreakdownCard(order)

            if (rental != null && hasDeposit) {
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    depositRefunded -> DepositStatusCard(
                        text = if (isBuyerSide) {
                            "Deposit Refunded to Wallet: ${formatMoney(rental.deposit)}"
                        } else {
                            "✓ Security Deposit Refunded (${formatMoney(rental.deposit)})"
                        },
                        color = SuccessGreen
                    )
                    depositHolding && isBuyerSide -> DepositStatusCard(
                        text = "Deposit Held in Escrow: ${formatMoney(rental.deposit)}",
                        color = WarningAmber
                    )
                    depositHolding && isSellerSide && !canReleaseDeposit -> DepositStatusCard(
                        text = "Security Deposit Held: ${formatMoney(rental.deposit)} · available after return inspection",
                        color = WarningAmber
                    )
                }
            }

            if (payoutPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))) {
                    Text(
                        "${formatMoney(estimatedPayout(order))} on hold — released once the ${if (order is RentalOrder) "item's return is confirmed" else "buyer confirms receipt"}.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (canReview && !alreadyReviewed) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onWriteReviewClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Leave a Review")
                }
            }

            if (canRequestReturn) {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.OutlinedButton(onClick = onRequestReturnClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Request Return/Refund")
                }
            }

            if (canReviewReturnRequest) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onReviewRequestClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Review Request")
                }
            }

            if (rental != null && isSellerSide && depositHolding) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { if (canReleaseDeposit) showReleaseDepositDialog = true },
                    enabled = canReleaseDeposit && !isReleasingDeposit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Inspect & Release Deposit (${formatMoney(rental.deposit)})")
                }
                if (!canReleaseDeposit) {
                    Text(
                        "Available after the returned item has been confirmed.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove this order?") },
            text = { Text("This removes it from your own history only — it won't affect the other party's record.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        OrderRepository.hideForCurrentUser(order)
                        showDeleteDialog = false
                        onDeleted()
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReleaseDepositDialog && rental != null) {
        AlertDialog(
            onDismissRequest = { if (!isReleasingDeposit) showReleaseDepositDialog = false },
            title = { Text("Release Security Deposit") },
            text = {
                Text(
                    "Confirm that the rental item has been safely returned without damage. " +
                        "${formatMoney(rental.deposit)} will be immediately refunded to the renter's wallet."
                )
            },
            confirmButton = {
                Button(
                    enabled = !isReleasingDeposit,
                    onClick = {
                        isReleasingDeposit = true
                        scope.launch {
                            OrderRepository.releaseRentalDeposit(order, context)
                                .onSuccess {
                                    showReleaseDepositDialog = false
                                    snackbarHostState.showSnackbar("Security deposit refunded")
                                }
                                .onFailure { error ->
                                    snackbarHostState.showSnackbar(error.message ?: "Couldn't release the deposit")
                                }
                            isReleasingDeposit = false
                        }
                    }
                ) {
                    if (isReleasingDeposit) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Release & Refund")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isReleasingDeposit,
                    onClick = { showReleaseDepositDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    pendingTrackTap?.let { (courierLabel, trackingNumber) ->
        val courier = Courier.entries.firstOrNull { it.label == courierLabel }
        val canAutoFill = courier?.tracksAutomatically() == true
        AlertDialog(
            onDismissRequest = { pendingTrackTap = null },
            title = { Text("Track this shipment?") },
            text = {
                Text(
                    if (canAutoFill) {
                        "This copies $trackingNumber to your clipboard and opens $courierLabel's tracking page with it already filled in."
                    } else {
                        "This copies $trackingNumber to your clipboard and opens $courierLabel's tracking page — paste it into their search box to check the status."
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    performTrackTap(courierLabel, trackingNumber)
                    pendingTrackTap = null
                }) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTrackTap = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DepositStatusCard(text: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProductHeaderCard(order: Order, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = order.productImage,
                contentDescription = order.productTitle,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(order.productTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
                StatusBadge(order.status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: OrderStatus) {
    val color = when (status) {
        OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING -> WarningAmber
        OrderStatus.CONFIRMED, OrderStatus.PAID, OrderStatus.PROCESSING -> MaterialTheme.colorScheme.primary
        OrderStatus.ACTIVE, OrderStatus.READY_FOR_HANDOVER, OrderStatus.RENTING, OrderStatus.RETURN_PENDING -> BrandOrange
        OrderStatus.COMPLETED, OrderStatus.RETURNED -> SuccessGreen
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
        OrderStatus.SHIPPED, OrderStatus.RENTAL_SHIPPED, OrderStatus.RETURN_AWAITING_SHIP, OrderStatus.RETURN_AWAITING_RECEIPT -> BrandOrange
        OrderStatus.TO_REVIEW -> WarningAmber
        OrderStatus.REFUNDED -> SuccessGreen
        OrderStatus.RETURN_REQUESTED -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status.label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FulfillmentDetail(
    label: String,
    method: FulfillmentMethod,
    meetup: com.example.gadgetmover.model.MeetupLocation?,
    receiverName: String?,
    phoneNumber: String?,
    fullAddress: String?,
    courier: String? = null,
    trackingNumber: String? = null,
    onOpenMaps: (com.example.gadgetmover.model.MeetupLocation) -> Unit,
    onTrackTap: (courierLabel: String, trackingNumber: String) -> Unit = { _, _ -> }
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(method.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        when (method) {
            FulfillmentMethod.MEETUP -> if (meetup != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meetup.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(meetup.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    TextButton(onClick = { onOpenMaps(meetup) }) { Text("Open in Maps") }
                }
            }
            FulfillmentMethod.SHIPPING -> {
                Spacer(modifier = Modifier.height(6.dp))
                if (receiverName != null && fullAddress != null) {
                    Text("$receiverName · ${phoneNumber.orEmpty()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(fullAddress, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Address details unavailable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (courier != null && trackingNumber != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$courier · $trackingNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier.clickable { onTrackTap(courier, trackingNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceBreakdownCard(order: Order) {
    val checkout = order.checkout
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (order) {
                is BuyOrder -> {
                    val itemSubtotal = order.price - checkout.platformFee - checkout.shippingFee + checkout.voucherDiscount
                    BreakdownRow("Item Subtotal", formatMoney(itemSubtotal))
                }
                is RentalOrder -> {
                    BreakdownRow("Rental Subtotal", "${formatMoney(order.dailyRate)} × ${order.days} = ${formatMoney(order.dailyRate * order.days)}")
                }
            }
            BreakdownRow("Platform Protection Fee", formatMoney(checkout.platformFee))
            if (checkout.shippingFee > 0) BreakdownRow("Shipping Fee", formatMoney(checkout.shippingFee))
            if (order is RentalOrder && order.deposit > 0) {
                BreakdownRow("Refundable Deposit", "${formatMoney(order.deposit)} · ${checkout.depositStatus?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Held"}")
            }
            if (checkout.voucherDiscount > 0) BreakdownRow("Voucher Discount", "-${formatMoney(checkout.voucherDiscount)}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Paid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatMoney(if (order is BuyOrder) order.price else (order as RentalOrder).totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            order.paymentId?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Payment ref: ${it.takeLast(8)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(millis))

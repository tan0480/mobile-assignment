package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.OrderRepository
import com.example.gadgetmover.data.ReviewRepository
import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.OrderStatus
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.screen.components.ReviewDialog
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.ui.theme.SuccessGreen
import com.example.gadgetmover.ui.theme.WarningAmber
import com.example.gadgetmover.util.estimatedPayout
import com.example.gadgetmover.util.formatDisplayDate
import com.example.gadgetmover.util.formatMoney
import com.example.gadgetmover.util.openInGoogleMaps
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
    onRequestReturnClick: () -> Unit = {},
    onReviewRequestClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var alreadyReviewed by remember { mutableStateOf(true) }

    val isBuyerSide = when (order) {
        is BuyOrder -> order.isPurchase
        is RentalOrder -> order.isRenter
    }
    val isSellerSide = !isBuyerSide
    val canReview = order.status == OrderStatus.TO_REVIEW && isBuyerSide
    val canRequestReturn = order is BuyOrder && isBuyerSide && order.status == OrderStatus.SHIPPED
    val canReviewReturnRequest = order is BuyOrder && isSellerSide && order.status == OrderStatus.RETURN_REQUESTED
    val payoutPending = when (order) {
        is BuyOrder -> order.status == OrderStatus.SHIPPED
        is RentalOrder -> order.status in setOf(OrderStatus.RENTAL_SHIPPED, OrderStatus.RENTING, OrderStatus.RETURN_PENDING)
    }

    LaunchedEffect(order.id) {
        if (canReview) alreadyReviewed = ReviewRepository.hasReviewed(order.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove from history")
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
            ProductHeaderCard(order)
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Order Info")
            Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    InfoRow("Order ID", "#${order.id.take(8).uppercase()}")
                    InfoRow("Status", order.status.label)
                    InfoRow("Payment", order.paymentStatus.name.lowercase().replaceFirstChar { it.uppercase() })
                    InfoRow("With", order.counterpartyName)
                    InfoRow("Order Date", formatDisplayDate(order.createdDate))
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
                        onOpenMaps = { openInGoogleMaps(context, it) }
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
                            onOpenMaps = { openInGoogleMaps(context, it) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Price Breakdown")
            PriceBreakdownCard(order)

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
                    onClick = { showReviewDialog = true },
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
        }
    }

    if (showReviewDialog) {
        ReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                scope.launch {
                    if (ReviewRepository.submitReview(order.id, rating, comment)) {
                        alreadyReviewed = true
                        OrderRepository.refreshFromRemote()
                    }
                    showReviewDialog = false
                }
            }
        )
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
}

@Composable
private fun ProductHeaderCard(order: Order) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
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
    onOpenMaps: (com.example.gadgetmover.model.MeetupLocation) -> Unit
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
                    Text("$courier · $trackingNumber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

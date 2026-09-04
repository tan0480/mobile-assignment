package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.OrderRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.OrderStatus
import com.example.gadgetmover.model.PaymentRecordStatus
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.ui.theme.SuccessGreen
import com.example.gadgetmover.util.estimatedPayout
import com.example.gadgetmover.util.formatMoney

/**
 * True once this order's payout has actually landed in the seller's/owner's wallet — the
 * `release_order_payout` trigger (schema.sql) fires exactly on the transition into TO_REVIEW
 * (BUY: buyer confirmed receipt; RENT: owner confirmed the item's return), and COMPLETED is
 * always reached *from* TO_REVIEW afterward, so together they're precisely "money already
 * released." PAID/SHIPPED/RENTAL_SHIPPED/RENTING/RETURN_PENDING orders haven't paid out yet;
 * CANCELLED and the return/refund-dispute statuses (RETURN_REQUESTED and onward, REFUNDED) never
 * released a payout to begin with — a return dispute can only be opened before the order reaches
 * TO_REVIEW, so there's no risk of double-counting an order that paid out and later got refunded.
 */
private fun Order.payoutReleased(): Boolean =
    paymentStatus == PaymentRecordStatus.PAID && status in setOf(OrderStatus.TO_REVIEW, OrderStatus.COMPLETED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBackClick: () -> Unit) {
    val user = AuthRepository.currentUser.value
    val myListings = ProductRepository.myListings(user?.id.orEmpty())
    val sales = OrderRepository.mySales()
    val activeRentals = OrderRepository.myLeases().count { it.status == OrderStatus.ACTIVE }

    // Net revenue — what actually landed in the wallet, not the raw amount the buyer/renter paid:
    // estimatedPayout() (same formula the real release_order_payout SQL trigger uses) already
    // strips the refundable rental deposit and the platform fee out of totalAmount/price. Only
    // orders whose payout has actually released count at all, so an order still mid-flight (or
    // cancelled/refunded before ever paying out) can't inflate this number.
    val paidSales = sales.filter { it.payoutReleased() }
    val paidLeases = OrderRepository.myLeases().filter { it.payoutReleased() }
    val salesRevenue = paidSales.sumOf { estimatedPayout(it) }
    val rentalRevenue = paidLeases.sumOf { estimatedPayout(it) }
    val netRevenue = salesRevenue + rentalRevenue

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("This month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icon = Icons.Filled.Inventory2,
                    tint = Color(0xFF7C3AED),
                    label = "Active listings",
                    value = myListings.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.ShoppingCart,
                    tint = BrandBlueDark,
                    label = "Items sold",
                    value = sales.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icon = Icons.Filled.Schedule,
                    tint = Color(0xFF2563EB),
                    label = "Active rentals",
                    value = activeRentals.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.Payments,
                    tint = SuccessGreen,
                    label = "Net Revenue",
                    value = formatMoney(netRevenue),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${formatMoney(salesRevenue)} from sales · ${formatMoney(rentalRevenue)} from rentals",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "* Excludes refundable rental deposits and platform fees, and only counts orders whose payout has actually released — cancelled or still-in-progress orders aren't included.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Seller rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (user?.rating ?: 0f).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "from ${user?.ratingCount ?: 0} reviews across ${myListings.size} listings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, tint: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

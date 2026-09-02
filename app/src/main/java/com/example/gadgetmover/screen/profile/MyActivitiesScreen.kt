package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.OrderRepository
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.model.BuyActivityTab
import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.OrderStatus
import com.example.gadgetmover.model.RentActivityTab
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.screen.components.ShipmentDialog
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.ui.theme.SuccessGreen
import com.example.gadgetmover.ui.theme.WarningAmber
import com.example.gadgetmover.util.Courier
import com.example.gadgetmover.util.formatMoney
import kotlinx.coroutines.launch

private val tabTitles = listOf("Purchases", "Sales", "Rentals", "Leases")

/** One card action a buyer/seller can take on an order — mirrors the transition tables enforced server-side by `advance_order_status`/`mark_order_shipped`, so a button never offers a move the backend would reject. [StatusChange] reuses the generic confirm dialog + `advanceStatus`; [Ship] opens [ShipmentDialog] + calls `markShipped`. */
private sealed class OrderAction(val label: String, val destructive: Boolean = false) {
    class StatusChange(label: String, val newStatus: OrderStatus, destructive: Boolean = false) : OrderAction(label, destructive)
    class Ship(label: String, val method: FulfillmentMethod) : OrderAction(label)
}

/** Whether releasing/crediting funds happens on this transition — the caller must refresh [WalletRepository] afterward if so. */
private fun OrderStatus.releasesPayout(): Boolean = this == OrderStatus.TO_REVIEW || this == OrderStatus.REFUNDED

private fun actionsFor(order: Order): List<OrderAction> {
    val isBuyerSide = when (order) {
        is BuyOrder -> order.isPurchase
        is RentalOrder -> order.isRenter
    }
    val isSellerSide = !isBuyerSide
    val status = order.status
    val actions = mutableListOf<OrderAction>()
    when (order) {
        is BuyOrder -> {
            if (isSellerSide && status == OrderStatus.PAID) actions += OrderAction.Ship("Mark as Shipped", order.checkout.receivingMethod)
            if (isBuyerSide && status == OrderStatus.SHIPPED) actions += OrderAction.StatusChange("Confirm Received", OrderStatus.TO_REVIEW)
            if ((isBuyerSide || isSellerSide) && (status == OrderStatus.PAID || status == OrderStatus.SHIPPED)) {
                actions += OrderAction.StatusChange("Cancel Order", OrderStatus.CANCELLED, destructive = true)
            }
            if (isBuyerSide && status == OrderStatus.RETURN_AWAITING_SHIP) {
                actions += OrderAction.Ship("Ship Return", order.checkout.returningMethod ?: FulfillmentMethod.MEETUP)
            }
            if (isSellerSide && status == OrderStatus.RETURN_AWAITING_RECEIPT) {
                actions += OrderAction.StatusChange("Confirm Return Received", OrderStatus.REFUNDED)
            }
        }
        is RentalOrder -> {
            if (isSellerSide && status == OrderStatus.PAID) actions += OrderAction.Ship("Mark as Shipped", order.checkout.receivingMethod)
            if (isBuyerSide && status == OrderStatus.RENTAL_SHIPPED) actions += OrderAction.StatusChange("Confirm Received", OrderStatus.RENTING)
            if ((isBuyerSide || isSellerSide) && (status == OrderStatus.PAID || status == OrderStatus.RENTAL_SHIPPED)) {
                actions += OrderAction.StatusChange("Cancel Order", OrderStatus.CANCELLED, destructive = true)
            }
            if (isBuyerSide && status == OrderStatus.RENTING) {
                actions += OrderAction.Ship("Start Return", order.checkout.returningMethod ?: FulfillmentMethod.MEETUP)
            }
            if (isSellerSide && status == OrderStatus.RETURN_PENDING) {
                actions += OrderAction.StatusChange("Confirm Item Returned", OrderStatus.TO_REVIEW)
            }
        }
    }
    return actions
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActivitiesScreen(onBackClick: () -> Unit, onOrderClick: (Order) -> Unit, initialTab: Int = 0) {
    // rememberSaveable (not remember) so the tab/filter survive a round trip into Order Details
    // and back — this composable is fully disposed while that screen is on top, so plain
    // `remember` would reset to [initialTab] every time regardless of what was selected before.
    var selectedTab by rememberSaveable { mutableStateOf(initialTab.coerceIn(0, tabTitles.lastIndex)) }
    var selectedBuyTab by rememberSaveable { mutableStateOf(BuyActivityTab.ALL) }
    var selectedRentTab by rememberSaveable { mutableStateOf(RentActivityTab.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<Pair<Order, OrderAction.StatusChange>?>(null) }
    var pendingShipment by remember { mutableStateOf<Pair<Order, OrderAction.Ship>?>(null) }
    var pendingDelete by remember { mutableStateOf<Order?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isBuyContext = selectedTab == 0 || selectedTab == 1
    val orders: List<Order> = when (selectedTab) {
        0 -> OrderRepository.myPurchases()
        1 -> OrderRepository.mySales()
        2 -> OrderRepository.myRentals()
        else -> OrderRepository.myLeases()
    }
    val activeStatuses = if (isBuyContext) selectedBuyTab.statuses else selectedRentTab.statuses
    val filteredOrders = activeStatuses?.let { set -> orders.filter { it.status in set } } ?: orders

    // Refreshes both on first entry and on every ON_RESUME (not just LaunchedEffect(Unit)) so
    // coming back from Order Details or elsewhere without this composable being disposed still
    // picks up any status change the other party made in the meantime.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { OrderRepository.refreshFromRemote() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("My Activities") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                SecondaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                selectedBuyTab = BuyActivityTab.ALL
                                selectedRentTab = RentActivityTab.ALL
                            },
                            text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
                if (isBuyContext) {
                    StatusFilterRow(tabs = BuyActivityTab.entries, selected = selectedBuyTab, labelOf = { it.label }, onSelect = { selectedBuyTab = it })
                } else {
                    StatusFilterRow(tabs = RentActivityTab.entries, selected = selectedRentTab, labelOf = { it.label }, onSelect = { selectedRentTab = it })
                }
            }
        }
    ) { padding ->
        AppPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    OrderRepository.refreshFromRemote()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders) { order ->
                        OrderCard(
                            order,
                            onAction = { action ->
                                when (action) {
                                    is OrderAction.StatusChange -> pendingAction = order to action
                                    is OrderAction.Ship -> pendingShipment = order to action
                                }
                            },
                            onClick = { onOrderClick(order) },
                            onDeleteClick = { pendingDelete = order }
                        )
                    }
                }
            }
        }
        }
    }

    pendingAction?.let { (order, action) ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(action.label) },
            text = {
                Text(
                    if (action.destructive) "This will cancel the order and cannot be undone."
                    else "Mark this order as \"${action.newStatus.label}\"?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val succeeded = OrderRepository.advanceStatus(order, action.newStatus)
                        pendingAction = null
                        if (succeeded) {
                            OrderRepository.refreshFromRemote()
                            if (action.newStatus.releasesPayout()) WalletRepository.refreshFromRemote()
                        } else {
                            snackbarHostState.showSnackbar("Couldn't update this order. Please try again.")
                        }
                    }
                }) { Text(action.label) }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Back") }
            }
        )
    }

    pendingShipment?.let { (order, action) ->
        ShipmentDialog(
            method = action.method,
            title = action.label,
            onDismiss = { pendingShipment = null },
            onConfirm = { courier, tracking ->
                scope.launch {
                    val succeeded = OrderRepository.markShipped(order, courier, tracking)
                    pendingShipment = null
                    if (!succeeded) snackbarHostState.showSnackbar("Couldn't update this order. Please try again.")
                }
            }
        )
    }

    pendingDelete?.let { order ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this order?") },
            text = { Text("This removes it from your own history only — it won't affect the other party's record.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val succeeded = OrderRepository.hideForCurrentUser(order)
                        pendingDelete = null
                        if (!succeeded) snackbarHostState.showSnackbar("Couldn't remove this order. Please try again.")
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun <T> StatusFilterRow(tabs: List<T>, selected: T, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { tab ->
            StatusPillChip(label = labelOf(tab), selected = tab == selected, onClick = { onSelect(tab) })
        }
    }
}

@Composable
private fun StatusPillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun OrderCard(order: Order, onAction: (OrderAction) -> Unit, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = order.productImage,
                    contentDescription = order.productTitle,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.productTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        "with ${order.counterpartyName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    when (order) {
                        is BuyOrder -> Text(
                            formatMoney(order.price),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        is RentalOrder -> Text(
                            "${formatMoney(order.totalAmount)} · ${order.days} days",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(order.status)
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove from history",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            val actions = actionsFor(order)
            if (actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { action ->
                        if (action.destructive) {
                            OutlinedButton(onClick = { onAction(action) }, modifier = Modifier.weight(1f)) {
                                Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            Button(onClick = { onAction(action) }, modifier = Modifier.weight(1f)) {
                                Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status.label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

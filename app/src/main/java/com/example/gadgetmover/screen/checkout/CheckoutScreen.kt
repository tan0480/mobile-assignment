package com.example.gadgetmover.screen.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.gadgetmover.BuildConfig
import com.example.gadgetmover.data.AddressRepository
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.model.Address
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.screen.components.PasswordConfirmDialog
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.formatMoney
import com.example.gadgetmover.util.openInGoogleMaps
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    product: Product,
    transactionType: ListingType,
    onBackClick: () -> Unit,
    onChangeAddress: () -> Unit,
    onOrderConfirmed: (Order) -> Unit,
    pickedAddressId: String? = null,
    negotiatedPrice: Double? = null
) {
    val context = LocalContext.current
    val viewModel: CheckoutViewModel = viewModel(
        factory = remember(product.id, transactionType, negotiatedPrice) { checkoutViewModelFactory(product, transactionType, negotiatedPrice) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val clientSecret by viewModel.clientSecretToPresent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY)
    }

    val paymentSheet = PaymentSheet.Builder(resultCallback = viewModel::onPaymentSheetResult).build()

    LaunchedEffect(clientSecret) {
        clientSecret?.let { secret ->
            paymentSheet.presentWithPaymentIntent(
                secret,
                PaymentSheet.Configuration(merchantDisplayName = "Gadget Mover")
            )
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    // Result of tapping "Change Address" (Screen.SelectShippingAddress), delivered back via the
    // Checkout back stack entry's own SavedStateHandle — see NavGraph's wiring for that route.
    LaunchedEffect(pickedAddressId) {
        pickedAddressId?.let { id -> AddressRepository.addresses.find { it.id == id }?.let(viewModel::selectAddress) }
    }

    // On success, skip any confirmation popup and go straight to the full Order Confirmed screen —
    // there's nothing left for the buyer to decide, so a blocking dialog would just be an extra tap.
    // Failure/cancellation still surfaces as a blocking popup so the buyer sees what happened and
    // picks what to do next; dismissedForState re-arms it for a *new* failure rather than looping.
    LaunchedEffect(uiState.paymentState, uiState.createdOrder) {
        if (uiState.paymentState is PaymentState.Success) {
            uiState.createdOrder?.let(onOrderConfirmed)
        }
    }
    var dismissedForState by remember { mutableStateOf<PaymentState?>(null) }
    val showFailureDialog = (uiState.paymentState is PaymentState.Failed || uiState.paymentState is PaymentState.Cancelled) &&
        dismissedForState != uiState.paymentState

    var showRentalDatePicker by remember { mutableStateOf(false) }
    var showWalletPasswordDialog by remember { mutableStateOf(false) }

    // Address selection isn't observed reactively — re-pull the default address whenever this
    // screen resumes (e.g. returning from "Change Address" on the Shipping Addresses screen).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshSelectedAddress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: swap with custom ImageVector
                    }
                }
            )
        },
        bottomBar = {
            CheckoutBottomBar(
                uiState = uiState,
                onPayClick = {
                    if (uiState.paymentState.acceptsNewAttempt) {
                        if (uiState.orderCreationFailedAfterPayment || uiState.paymentState is PaymentState.Failed || uiState.paymentState is PaymentState.Cancelled) {
                            viewModel.retry()
                        } else if (uiState.paymentMethod == CheckoutPaymentMethod.WALLET) {
                            showWalletPasswordDialog = true
                        } else {
                            viewModel.startPayment()
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ProductSummaryCard(product, transactionType, negotiatedPrice)
            Spacer(modifier = Modifier.height(16.dp))

            if (transactionType == ListingType.RENT) {
                SectionTitle("Rental Period")
                RentalPeriodCard(
                    uiState = uiState,
                    onStartClick = { showRentalDatePicker = true },
                    onDurationChange = viewModel::selectRentalDuration
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionTitle("Receiving Method")
            FulfillmentMethodRow(
                available = product.fulfillmentMethods,
                selected = uiState.receivingMethod,
                onSelect = viewModel::selectReceivingMethod
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (transactionType == ListingType.RENT) {
                SectionTitle("Returning Method")
                FulfillmentMethodRow(
                    available = product.fulfillmentMethods,
                    selected = uiState.returningMethod,
                    onSelect = viewModel::selectReturningMethod
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.receivingMethod == FulfillmentMethod.SHIPPING || (transactionType == ListingType.RENT && uiState.returningMethod == FulfillmentMethod.SHIPPING)) {
                SectionTitle("Delivery Speed")
                ShippingTierCards(product = product, selected = uiState.shippingTier, onSelect = viewModel::selectShippingTier)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.needsAddress) {
                SectionTitle("Shipping Address")
                AddressCard(uiState.selectedAddress, onChangeClick = onChangeAddress)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.needsReceivingMeetup) {
                SectionTitle("Receiving — Meet-up Location")
                MeetupLocationCards(
                    locations = product.meetupLocations,
                    selected = uiState.receivingMeetup,
                    onSelect = viewModel::selectReceivingMeetup,
                    onOpenMaps = { openInGoogleMaps(context, it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.needsReturningMeetup) {
                SectionTitle("Returning — Meet-up Location")
                MeetupLocationCards(
                    locations = product.meetupLocations,
                    selected = uiState.returningMeetup,
                    onSelect = viewModel::selectReturningMeetup,
                    onOpenMaps = { openInGoogleMaps(context, it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionTitle("Price Breakdown")
            PriceBreakdownCard(uiState, transactionType)
            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("Payment Method")
            PaymentMethodCards(selected = uiState.paymentMethod, onSelect = viewModel::selectPaymentMethod)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWalletPasswordDialog) {
        PasswordConfirmDialog(
            message = "Enter your password to pay ${formatMoney(uiState.finalTotal)} from your Gadget Mover Wallet.",
            onDismiss = { showWalletPasswordDialog = false },
            onConfirmed = {
                showWalletPasswordDialog = false
                viewModel.startPayment()
            }
        )
    }

    if (showRentalDatePicker) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.rentalStartMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    if (date.isBefore(today)) return false
                    return uiState.bookedRanges.none { booked -> !date.isBefore(booked.start) && !date.isAfter(booked.end) }
                }
            }
        )
        Dialog(onDismissRequest = { showRentalDatePicker = false }) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column {
                    DatePicker(state = datePickerState, modifier = Modifier.height(480.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRentalDatePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let(viewModel::selectRentalStart)
                            showRentalDatePicker = false
                        }) { Text("Confirm") }
                    }
                }
            }
        }
    }

    if (showFailureDialog) {
        val isCancelled = uiState.paymentState is PaymentState.Cancelled
        val reason = (uiState.paymentState as? PaymentState.Failed)?.reason ?: "Your payment was cancelled."
        AlertDialog(
            onDismissRequest = { dismissedForState = uiState.paymentState },
            title = { Text(if (isCancelled) "Payment Cancelled" else "Payment Failed") },
            text = { Text(reason) },
            confirmButton = {
                TextButton(onClick = {
                    dismissedForState = uiState.paymentState
                    viewModel.retry()
                }) { Text("Retry") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dismissedForState = uiState.paymentState
                    onBackClick()
                }) { Text("Back to Product") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ProductSummaryCard(product: Product, transactionType: ListingType, negotiatedPrice: Double? = null) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = product.title,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    "${product.category.label} · ${product.condition.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val originalPrice = if (transactionType == ListingType.RENT) product.rentalRatePerDay ?: 0.0 else product.price ?: 0.0
                val suffix = if (transactionType == ListingType.RENT) " / day" else ""
                if (negotiatedPrice != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatMoney(originalPrice)}$suffix",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${formatMoney(negotiatedPrice)}$suffix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        "${formatMoney(originalPrice)}$suffix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RentalPeriodCard(uiState: CheckoutUiState, onStartClick: () -> Unit, onDurationChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onStartClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) // TODO: swap with custom ImageVector
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        if (uiState.rentalStartMillis != null) "Starts ${formatDate(uiState.rentalStartMillis)}" else "Select a start date",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (uiState.rentalEndMillis != null) {
                        Text(
                            "Ends ${formatDate(uiState.rentalEndMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Duration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onDurationChange((uiState.rentalDuration - 1).coerceAtLeast(1)) },
                    enabled = uiState.rentalDuration > 1
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease duration")
                }
                Text(
                    "${uiState.rentalDuration.coerceAtLeast(1)} day${if (uiState.rentalDuration == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { onDurationChange((uiState.rentalDuration + 1).coerceAtMost(90)) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase duration")
                }
            }
            if (uiState.hasDateConflict) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) // TODO: swap with custom ImageVector
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "These dates overlap with an existing booking. Please pick a different start date or duration.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (uiState.receivingMethod == FulfillmentMethod.SHIPPING) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your rental period starts once the item is delivered, not on the date above.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FulfillmentMethodRow(available: Set<FulfillmentMethod>, selected: FulfillmentMethod?, onSelect: (FulfillmentMethod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        available.forEach { method ->
            SelectableCard(title = method.label, isSelected = selected == method, onClick = { onSelect(method) })
        }
    }
}

@Composable
private fun ShippingTierCards(product: Product, selected: ShippingTier, onSelect: (ShippingTier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShippingTier.entries.forEach { tier ->
            val sellerFee = when (tier) {
                ShippingTier.STANDARD -> product.standardShippingFee
                ShippingTier.EXPRESS -> product.expressShippingFee
            }
            if (sellerFee != null) {
                SelectableCard(
                    title = tier.label,
                    subtitle = tier.etaLabel,
                    trailing = formatMoney(sellerFee),
                    isSelected = selected == tier,
                    onClick = { onSelect(tier) }
                )
            }
        }
    }
}

@Composable
private fun AddressCard(address: Address?, onChangeClick: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) // TODO: swap with custom ImageVector
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (address == null) {
                    Text("No address selected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("${address.receiverName}   ${address.phoneNumber}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(address.fullAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            TextButton(onClick = onChangeClick) { Text(if (address == null) "Add" else "Change") }
        }
    }
}

@Composable
private fun MeetupLocationCards(
    locations: List<MeetupLocation>,
    selected: MeetupLocation?,
    onSelect: (MeetupLocation) -> Unit,
    onOpenMaps: (MeetupLocation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        locations.forEach { location ->
            Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(location) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected?.id == location.id, onClick = { onSelect(location) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(location.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(location.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    TextButton(onClick = { onOpenMaps(location) }) { Text("Open in Maps") }
                }
            }
        }
    }
}

@Composable
private fun PriceBreakdownCard(uiState: CheckoutUiState, transactionType: ListingType) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (transactionType == ListingType.RENT) {
                BreakdownRow("Rental Subtotal", formatMoney(uiState.rentalSubtotal))
            } else {
                BreakdownRow("Item Subtotal", formatMoney(uiState.itemSubtotal))
            }
            BreakdownRow("Platform Protection Fee", formatMoney(uiState.platformFee))
            if (uiState.shippingFee > 0) BreakdownRow("Shipping Fee", formatMoney(uiState.shippingFee))
            if (transactionType == ListingType.RENT && uiState.refundableDeposit > 0) {
                BreakdownRow("Refundable Deposit", formatMoney(uiState.refundableDeposit))
            }
            if (uiState.voucherDiscount > 0) BreakdownRow("Voucher Discount", "-${formatMoney(uiState.voucherDiscount)}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Final Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatMoney(uiState.finalTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (transactionType == ListingType.RENT && uiState.refundableDeposit > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Includes a refundable deposit — held, not a fee, and returned after the item comes back.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun PaymentMethodCards(selected: CheckoutPaymentMethod, onSelect: (CheckoutPaymentMethod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CheckoutPaymentMethod.entries.forEach { method ->
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!method.isAvailable) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = method.isAvailable) { onSelect(method) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == method, onClick = { onSelect(method) }, enabled = method.isAvailable)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            method.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (method.isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (method == CheckoutPaymentMethod.WALLET) {
                            Text(
                                "Balance: ${formatMoney(WalletRepository.balance.value)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!method.isAvailable) {
                        Text("Coming Soon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableCard(title: String, subtitle: String? = null, trailing: String? = null, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) BrandOrange else MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CheckoutBottomBar(uiState: CheckoutUiState, onPayClick: () -> Unit) {
    Card(shape = RoundedCornerShape(0.dp), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val statusText = when (val state = uiState.paymentState) {
                PaymentState.Idle -> null
                PaymentState.CreatingPayment -> "Preparing payment..."
                PaymentState.PaymentReady -> "Opening payment sheet..."
                PaymentState.Processing -> "Processing payment..."
                PaymentState.Success -> "Payment Successful"
                is PaymentState.Failed -> "Payment Failed — ${state.reason}"
                PaymentState.Cancelled -> "Payment Cancelled"
                PaymentState.Pending -> "Payment is still being processed."
                PaymentState.Expired -> "Payment session expired — please try again."
            }
            if (statusText != null) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.paymentState is PaymentState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            val isBusy = uiState.paymentState is PaymentState.CreatingPayment ||
                uiState.paymentState is PaymentState.PaymentReady ||
                uiState.paymentState is PaymentState.Processing
            Button(
                onClick = onPayClick,
                enabled = uiState.isReadyToPay,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    val label = if (uiState.orderCreationFailedAfterPayment || uiState.paymentState is PaymentState.Failed || uiState.paymentState is PaymentState.Cancelled) "Retry" else "Pay"
                    Text("$label ${formatMoney(uiState.finalTotal)}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(millis))

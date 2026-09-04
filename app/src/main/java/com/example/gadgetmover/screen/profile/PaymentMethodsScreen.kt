package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.BuildConfig
import com.example.gadgetmover.data.CheckoutRepository
import com.example.gadgetmover.model.PaymentMethod
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.ui.theme.SuccessGreen
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch

/**
 * Real, Stripe-backed saved cards (Stripe Customer + PaymentMethod list) rather than a local
 * mock — this list is what checkout's PaymentSheet also reads/writes, so a card added or used
 * here shows up there and vice versa. See [CheckoutRepository] for the Stripe/Edge Function calls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var methods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isAddingCard by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            isLoading = true
            CheckoutRepository.listPaymentMethods().fold(
                onSuccess = { result ->
                    methods = result.map { PaymentMethod(it.id, it.brand, it.last4, it.expMonth, it.expYear, it.isDefault) }
                },
                onFailure = { scope.launch { snackbarHostState.showSnackbar("Couldn't load payment methods.") } }
            )
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        refresh()
    }

    val paymentSheet = PaymentSheet.Builder(resultCallback = { result ->
        isAddingCard = false
        when (result) {
            is PaymentSheetResult.Completed -> refresh()
            is PaymentSheetResult.Canceled -> {}
            is PaymentSheetResult.Failed -> scope.launch { snackbarHostState.showSnackbar("Couldn't save card. Please try again.") }
        }
    }).build()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isAddingCard = true
                    scope.launch {
                        val customer = CheckoutRepository.getOrCreateStripeCustomer().getOrNull()
                        if (customer == null) {
                            isAddingCard = false
                            snackbarHostState.showSnackbar("Couldn't start card setup.")
                            return@launch
                        }
                        CheckoutRepository.createSetupIntent().fold(
                            onSuccess = { info ->
                                paymentSheet.presentWithSetupIntent(
                                    info.clientSecret,
                                    PaymentSheet.Configuration(
                                        merchantDisplayName = "Gadget Mover",
                                        customer = PaymentSheet.CustomerConfiguration(
                                            id = customer.customerId,
                                            ephemeralKeySecret = customer.ephemeralKey
                                        )
                                    )
                                )
                            },
                            onFailure = {
                                isAddingCard = false
                                snackbarHostState.showSnackbar("Couldn't start card setup.")
                            }
                        )
                    }
                },
                containerColor = BrandBlueDark
            ) {
                if (isAddingCard) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "Add card", tint = Color.White)
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            methods.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No payment methods yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(methods, key = { it.id }) { method ->
                        PaymentMethodRow(
                            method = method,
                            onSetDefault = {
                                scope.launch {
                                    CheckoutRepository.setDefaultPaymentMethod(method.id).fold(
                                        onSuccess = { refresh() },
                                        onFailure = { snackbarHostState.showSnackbar("Couldn't set default.") }
                                    )
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    CheckoutRepository.detachPaymentMethod(method.id).fold(
                                        onSuccess = { refresh() },
                                        onFailure = { snackbarHostState.showSnackbar("Couldn't remove card.") }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(method: PaymentMethod, onSetDefault: () -> Unit, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandBlueDark.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = BrandBlueDark)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${method.brand.replaceFirstChar { it.uppercase() }} •••• ${method.last4}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Expires ${method.expiry}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (method.isDefault) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Default", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            }
            if (!method.isDefault) {
                TextButton(onClick = onSetDefault) { Text("Set default") }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

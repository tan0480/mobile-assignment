package com.example.gadgetmover.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gadgetmover.BuildConfig
import com.example.gadgetmover.screen.checkout.PaymentState
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.util.formatMoney
import com.example.gadgetmover.util.sanitizeMoneyInput
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAddFundsAmountScreen(onBackClick: () -> Unit, onContinue: (Double) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Funds") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("How much would you like to add to your wallet?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeMoneyInput(it) },
                label = { Text("Amount (RM)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onContinue(amount) },
                enabled = amount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * The payment-method step of "Add Funds" — deliberately only ever offers one option (Credit or
 * Debit Card via Stripe), matching what this app actually supports, rather than implying choices
 * that don't exist yet. Tapping Pay creates a Stripe PaymentIntent, presents PaymentSheet, and —
 * only once the server (wallet-topup-confirm, see WalletTopUpViewModel/WalletRepository) has
 * re-verified the charge directly with Stripe — credits the wallet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAddFundsPaymentScreen(amount: Double, onBackClick: () -> Unit, onCompleted: () -> Unit) {
    val context = LocalContext.current
    val viewModel: WalletTopUpViewModel = viewModel(factory = remember(amount) { walletTopUpViewModelFactory(amount) })
    val paymentState by viewModel.paymentState.collectAsState()
    val clientSecret by viewModel.clientSecretToPresent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY)
    }
    val paymentSheet = PaymentSheet.Builder(resultCallback = viewModel::onPaymentSheetResult).build()
    LaunchedEffect(clientSecret) {
        clientSecret?.let { secret ->
            paymentSheet.presentWithPaymentIntent(secret, PaymentSheet.Configuration(merchantDisplayName = "Gadget Mover"))
        }
    }
    LaunchedEffect(paymentState) {
        // Navigate back immediately — showSnackbar() suspends until the snackbar is dismissed
        // (several seconds on its default duration), which was the actual cause of the "stuck"
        // delay before returning to the Wallet screen. The updated balance there is confirmation
        // enough, so no snackbar is needed here.
        if (paymentState is PaymentState.Success) {
            onCompleted()
        }
    }

    val busy = paymentState is PaymentState.CreatingPayment || paymentState is PaymentState.Processing
    val failed = paymentState as? PaymentState.Failed

    // While the server is verifying the charge and crediting the wallet, block navigation and
    // input entirely — this can take a couple of seconds, and without a clear blocking state the
    // screen just looks stuck, or a user could bail out and wrongly assume nothing happened.
    BackHandler(enabled = busy) {}

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Method") },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount to add", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Payment method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = true, onClick = {})
                    Text("Credit or Debit Card", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (failed != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(failed.reason, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { if (failed != null) viewModel.retry() else viewModel.startPayment() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = BrandBlueDark,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = androidx.compose.ui.graphics.Color.White)
                } else {
                    Text(if (failed != null) "Retry" else "Pay ${formatMoney(amount)}")
                }
            }
        }
    }
    if (busy) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center
        ) {
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Processing payment…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    }
}

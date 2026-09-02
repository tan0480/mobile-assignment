package com.example.gadgetmover.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.model.WalletTransactionType
import com.example.gadgetmover.screen.components.PasswordConfirmDialog
import com.example.gadgetmover.util.formatMoney
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletWithdrawAmountScreen(onBackClick: () -> Unit, onContinue: (Double) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val balance = WalletRepository.balance.value
    val exceedsBalance = amount > balance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdraw") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Available balance: ${formatMoney(balance)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("How much would you like to withdraw?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (RM)") },
                singleLine = true,
                isError = exceedsBalance,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (exceedsBalance) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Amount exceeds your wallet balance", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onContinue(amount) },
                enabled = amount > 0 && !exceedsBalance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * The "withdraw to where" step — collects a destination bank/card the same way Add Funds collects
 * a payment method, but doesn't actually move money anywhere: without a Malaysia-eligible Stripe
 * Connect platform account (Express connected accounts can't be created from a Malaysia-based
 * platform account — see the conversation that led here), there's no real payout rail to send it
 * through yet. Confirming just records the withdrawal against the wallet balance itself, the same
 * way it already did before this screen existed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletWithdrawDestinationScreen(amount: Double, onBackClick: () -> Unit, onCompleted: () -> Unit) {
    var bankName by remember { mutableStateOf("") }
    var accountHolderName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isValid = bankName.isNotBlank() && accountHolderName.isNotBlank() && accountNumber.length >= 6

    fun performWithdraw() {
        scope.launch {
            isSubmitting = true
            errorMessage = null
            val last4 = accountNumber.takeLast(4)
            val success = WalletRepository.debit(
                WalletTransactionType.WITHDRAWAL,
                amount,
                "Withdrawal to $bankName •••• $last4"
            )
            isSubmitting = false
            // Navigate back immediately — showSnackbar() suspends until the snackbar is dismissed
            // (several seconds on its default duration), which was the actual cause of the "stuck"
            // delay before returning to the Wallet screen. The updated balance there is
            // confirmation enough, so no snackbar is needed here.
            if (success) {
                onCompleted()
            } else {
                errorMessage = "Couldn't process this withdrawal. Please try again."
            }
        }
    }

    // While the withdrawal is being submitted, block navigation and input entirely — otherwise
    // the gap before this screen closes reads as the app being stuck rather than working.
    BackHandler(enabled = isSubmitting) {}

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdraw To") },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row {
                Text("Withdrawing ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Bank / card details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = accountHolderName,
                onValueChange = { accountHolderName = it },
                label = { Text("Account Holder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
                label = { Text("Card / Account Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { showPasswordDialog = true },
                enabled = isValid && !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm Withdrawal")
                }
            }
        }
    }
    if (isSubmitting) {
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
                    Text("Processing withdrawal…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    }

    if (showPasswordDialog) {
        PasswordConfirmDialog(
            message = "Enter your password to withdraw ${formatMoney(amount)} from your wallet.",
            onDismiss = { showPasswordDialog = false },
            onConfirmed = {
                showPasswordDialog = false
                performWithdraw()
            }
        )
    }
}

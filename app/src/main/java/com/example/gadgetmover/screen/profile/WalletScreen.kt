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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.model.WalletTransaction
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.model.WalletTransactionType
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.ui.theme.SuccessGreen
import com.example.gadgetmover.util.formatDisplayDate
import com.example.gadgetmover.util.formatMoney
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBackClick: () -> Unit,
    onAddFundsClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    successMessage: String? = null,
    onSuccessMessageShown: () -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        WalletRepository.refreshFromRemote()
    }

    // Add Funds / Withdraw navigate back here immediately on success (not blocked on a snackbar
    // on their own screen) and hand the confirmation message off via the nav back stack entry
    // instead — shown here once, then cleared so it doesn't reappear on a later recomposition.
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSuccessMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: swap with custom ImageVector
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AppPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    WalletRepository.refreshFromRemote()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandBlueDark)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Wallet, contentDescription = null, tint = Color.White) // TODO: swap with custom ImageVector
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Available balance", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    formatMoney(WalletRepository.balance.value),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Tighter contentPadding than the M3 default (24.dp horizontal) — with an icon
                    // plus a two-word label sharing half the row, the default padding left too
                    // little width for "Add funds" and it wrapped onto two lines at larger system
                    // font scales, while the shorter one-word "Withdraw" didn't — an uneven,
                    // broken-looking pair. maxLines = 1 backstops this so it can never silently
                    // wrap again; the extra padding room means it shouldn't need to clip either.
                    Button(
                        onClick = onAddFundsClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlueDark)
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) // TODO: swap with custom ImageVector
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add funds", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onWithdrawClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) // TODO: swap with custom ImageVector
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Withdraw", maxLines = 1)
                    }
                }
            }

            Text(
                "Transaction history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (WalletRepository.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) // TODO: swap with custom ImageVector
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(WalletRepository.transactions, key = { it.id }) { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    val isPositive = tx.amount >= 0
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background((if (isPositive) SuccessGreen else MaterialTheme.colorScheme.error).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPositive) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward, // TODO: swap with custom ImageVector
                    contentDescription = null,
                    tint = if (isPositive) SuccessGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(formatDisplayDate(tx.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${if (isPositive) "+" else ""}${formatMoney(tx.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) SuccessGreen else MaterialTheme.colorScheme.error
            )
        }
    }
}

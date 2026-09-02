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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.PaymentMethodRepository
import com.example.gadgetmover.model.PaymentMethod
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(onBackClick: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    val methods = PaymentMethodRepository.methods

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: swap with custom ImageVector
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = BrandBlueDark) {
                Icon(Icons.Filled.Add, contentDescription = "Add card", tint = Color.White) // TODO: swap with custom ImageVector
            }
        }
    ) { padding ->
        if (methods.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No payment methods yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(methods, key = { it.id }) { method ->
                    PaymentMethodRow(
                        method = method,
                        onSetDefault = { PaymentMethodRepository.setDefault(method.id) },
                        onRemove = { PaymentMethodRepository.remove(method.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPaymentMethodDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { brand, last4, expiry ->
                PaymentMethodRepository.add(brand, last4, expiry)
                showAddDialog = false
            }
        )
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
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = BrandBlueDark) // TODO: swap with custom ImageVector
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${method.brand} •••• ${method.last4}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) // TODO: swap with custom ImageVector
            }
        }
    }
}

@Composable
private fun AddPaymentMethodDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var brand by remember { mutableStateOf("Visa") }
    var last4 by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Card brand") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4) last4 = it.filter { c -> c.isDigit() } },
                    label = { Text("Last 4 digits") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { expiry = it },
                    label = { Text("Expiry (MM/YY)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(brand.ifBlank { "Card" }, last4.ifBlank { "0000" }, expiry.ifBlank { "--/--" }) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlueDark, contentColor = Color.White),
                enabled = last4.length == 4
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

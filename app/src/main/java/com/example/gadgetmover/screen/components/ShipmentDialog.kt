package com.example.gadgetmover.screen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.util.Courier
import com.example.gadgetmover.util.validate

/**
 * Reusable "mark this leg as shipped/handed over" dialog used for all 4 physical-handover
 * actions (BUY outbound, RENT outbound, RENT return leg, BUY return/refund leg). For SHIPPING,
 * requires a courier pick + a tracking number that validates against that courier's format
 * before Confirm enables; for MEETUP, it's a plain confirmation with no fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentDialog(
    method: FulfillmentMethod,
    title: String = "Mark as Shipped",
    onDismiss: () -> Unit,
    onConfirm: (courier: Courier?, trackingNumber: String?) -> Unit
) {
    if (method == FulfillmentMethod.MEETUP) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text("Mark this order as ready for handover?") },
            confirmButton = {
                Button(onClick = { onConfirm(null, null) }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedCourier by remember { mutableStateOf<Courier?>(null) }
    var trackingNumber by remember { mutableStateOf("") }
    val trackingValid = selectedCourier?.let { it.validate(trackingNumber) } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                // A plain tap-to-pick dropdown (readOnly field, no typed search) rather than an
                // editable text field — with only 7 fixed couriers there's nothing worth typing
                // to filter, and an editable OutlinedTextField inside ExposedDropdownMenuBox is
                // unreliable about raising the IME on real devices, leaving the field impossible
                // to type into on some phones.
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedCourier?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Courier") },
                        placeholder = { Text("Select courier") },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Courier.entries.forEach { courier ->
                            DropdownMenuItem(
                                text = { Text(courier.label) },
                                onClick = {
                                    selectedCourier = courier
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = trackingNumber,
                    onValueChange = { trackingNumber = it },
                    label = { Text("Tracking Number") },
                    singleLine = true,
                    isError = trackingNumber.isNotEmpty() && !trackingValid,
                    supportingText = {
                        when {
                            trackingNumber.isNotEmpty() && !trackingValid ->
                                Text("Doesn't match ${selectedCourier?.label ?: "the selected courier"}'s tracking number format", color = MaterialTheme.colorScheme.error)
                            selectedCourier != null -> Text(selectedCourier!!.formatHint)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCourier != null && trackingValid,
                onClick = { onConfirm(selectedCourier, trackingNumber.trim()) }
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

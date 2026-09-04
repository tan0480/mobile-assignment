package com.example.gadgetmover.screen.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.util.formatMoney
import com.example.gadgetmover.util.sanitizeMoneyInput

/**
 * Shown after picking a product for the "Special Price" attachment — enters a sale price and/or
 * a daily rental rate, depending on what [product] is listed for. Confirm enables once at least
 * one entered price is a valid positive amount; it isn't required to be below the original price.
 */
@Composable
fun OfferPriceDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (salePrice: Double?, rentalRate: Double?) -> Unit
) {
    var saleText by remember { mutableStateOf("") }
    var rentalText by remember { mutableStateOf("") }

    val showSale = product.listingType == ListingType.BUY || product.listingType == ListingType.BOTH
    val showRental = product.listingType == ListingType.RENT || product.listingType == ListingType.BOTH

    val originalSale = product.price ?: 0.0
    val originalRental = product.rentalRatePerDay ?: 0.0

    val saleValue = saleText.toDoubleOrNull()
    val rentalValue = rentalText.toDoubleOrNull()
    val saleValid = saleText.isBlank() || (saleValue != null && saleValue > 0.0)
    val rentalValid = rentalText.isBlank() || (rentalValue != null && rentalValue > 0.0)
    val hasAtLeastOne = (saleText.isNotBlank() && saleValid) || (rentalText.isNotBlank() && rentalValid)
    val canConfirm = saleValid && rentalValid && hasAtLeastOne

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Special Price") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = product.images.firstOrNull(),
                        contentDescription = product.title,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(product.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (showSale) {
                    Text(
                        "Original price: ${formatMoney(originalSale)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = saleText,
                        onValueChange = { saleText = sanitizeMoneyInput(it) },
                        label = { Text("Special sale price") },
                        isError = saleText.isNotBlank() && !saleValid,
                        supportingText = if (saleText.isNotBlank() && !saleValid) {
                            { Text("Enter a valid amount") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (showRental) {
                    Text(
                        "Original rate: ${formatMoney(originalRental)} / day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rentalText,
                        onValueChange = { rentalText = sanitizeMoneyInput(it) },
                        label = { Text("Special rate / day") },
                        isError = rentalText.isNotBlank() && !rentalValid,
                        supportingText = if (rentalText.isNotBlank() && !rentalValid) {
                            { Text("Enter a valid amount") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        if (saleText.isNotBlank() && saleValid) saleValue else null,
                        if (rentalText.isNotBlank() && rentalValid) rentalValue else null
                    )
                }
            ) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

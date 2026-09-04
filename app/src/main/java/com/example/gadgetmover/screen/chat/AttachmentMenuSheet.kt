package com.example.gadgetmover.screen.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.ui.theme.BrandOrange

/** The "+" attachment menu beside the chat input bar — one row per attachment type. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentMenuSheet(
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickLocation: () -> Unit,
    onPickProduct: () -> Unit,
    onPickSpecialPrice: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            AttachmentRow(Icons.Filled.Photo, "Photo") { onDismiss(); onPickPhoto() }
            AttachmentRow(Icons.Filled.LocationOn, "Send My Location") { onDismiss(); onPickLocation() }
            AttachmentRow(Icons.Filled.Sell, "Product") { onDismiss(); onPickProduct() }
            AttachmentRow(Icons.Filled.LocalOffer, "Special Price") { onDismiss(); onPickSpecialPrice() }
        }
    }
}

@Composable
private fun AttachmentRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BrandOrange)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

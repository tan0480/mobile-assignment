package com.example.gadgetmover.screen.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.ui.theme.BrandOrange

/** A tap-to-pick list of a listing's seller-declared meet-up spots — shared by Checkout and the return/refund flow. */
@Composable
fun MeetupLocationCards(
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
                    Spacer(modifier = Modifier.width(8.dp))
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

/** A generic selectable option card — shared by Checkout and the return/refund flow. [multiSelect]
 * swaps the leading [RadioButton] for a [Checkbox] for callers where more than one option can be
 * chosen at once (the buyer's own "which methods would you accept" step); single-select callers
 * (Checkout, the seller's final pick) leave it at the default. */
@Composable
fun SelectableCard(title: String, subtitle: String? = null, trailing: String? = null, isSelected: Boolean, onClick: () -> Unit, multiSelect: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) BrandOrange else MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (multiSelect) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                RadioButton(selected = isSelected, onClick = onClick)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Multi-select sibling of [MeetupLocationCards] — the buyer checks off every meet-up spot they'd
 * personally accept for a return, rather than picking just one. */
@Composable
fun MeetupLocationCheckboxCards(
    locations: List<MeetupLocation>,
    selected: Set<MeetupLocation>,
    onToggle: (MeetupLocation) -> Unit,
    onOpenMaps: (MeetupLocation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        locations.forEach { location ->
            Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(location) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = selected.any { it.id == location.id }, onCheckedChange = { onToggle(location) })
                    Spacer(modifier = Modifier.width(8.dp))
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

/** Prompts for a display name for a freshly map-picked spot before it's added to a meet-up list — shared by the listing wizard and the return/refund request form. */
@Composable
fun NameMeetupLocationDialog(address: String, initialName: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this meet-up spot") },
        text = {
            Column {
                Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("e.g. TAR UMT, KLCC LRT") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.ifBlank { "Meet-up spot" }) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

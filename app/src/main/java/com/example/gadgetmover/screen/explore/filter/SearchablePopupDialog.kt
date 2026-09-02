package com.example.gadgetmover.screen.explore.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.gadgetmover.model.filter.FilterFieldValue
import com.example.gadgetmover.model.filter.FilterOption
import com.example.gadgetmover.ui.theme.BrandOrange

/**
 * The modal popup behind [FilterType.SearchablePopupSelect][com.example.gadgetmover.model.filter.FilterType.SearchablePopupSelect]:
 * a top search bar filtering a scrollable option list, with an optional "use what I typed"
 * fallback row when [allowCustomInput] is true and the query matches no existing option
 * (e.g. an enthusiast switch model not yet in the catalogue).
 */
@Composable
fun SearchablePopupDialog(
    title: String,
    options: List<FilterOption>,
    isMultiSelect: Boolean,
    allowCustomInput: Boolean,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var draftSelection by remember { mutableStateOf(selectedIds) }

    val filteredOptions = remember(query, options) {
        if (query.isBlank()) options else options.filter { it.label.contains(query, ignoreCase = true) }
    }
    val exactMatchExists = remember(query, options) {
        query.isNotBlank() && options.any { it.label.equals(query, ignoreCase = true) }
    }

    fun toggle(id: String) {
        draftSelection = if (isMultiSelect) {
            if (draftSelection.contains(id)) draftSelection - id else draftSelection + id
        } else {
            setOf(id)
        }
        if (!isMultiSelect) {
            onConfirm(draftSelection)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.heightIn(max = 520.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    placeholder = { Text("Search $title") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    if (allowCustomInput && query.isNotBlank() && !exactMatchExists) {
                        item {
                            val customId = FilterFieldValue.customId(query)
                            OptionRow(
                                label = "Use \"$query\"",
                                isSelected = draftSelection.contains(customId),
                                isMultiSelect = isMultiSelect,
                                onClick = { toggle(customId) }
                            )
                        }
                    }
                    items(filteredOptions) { option ->
                        OptionRow(
                            label = option.label,
                            isSelected = draftSelection.contains(option.id),
                            isMultiSelect = isMultiSelect,
                            onClick = { toggle(option.id) }
                        )
                    }
                    if (filteredOptions.isEmpty() && !(allowCustomInput && query.isNotBlank())) {
                        item {
                            Text(
                                "No matches found",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (isMultiSelect) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { draftSelection = emptySet() }, modifier = Modifier.weight(1f)) {
                            Text("Clear")
                        }
                        Button(
                            onClick = { onConfirm(draftSelection); onDismiss() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelect) {
            Checkbox(checked = isSelected, onCheckedChange = { onClick() })
        } else {
            RadioButton(selected = isSelected, onClick = onClick)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (isSelected && isMultiSelect) {
            Box(
                modifier = Modifier
                    .background(BrandOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

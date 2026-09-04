package com.example.gadgetmover.screen.explore.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.model.filter.FilterFieldValue
import com.example.gadgetmover.model.filter.MAX_SWITCH_REQUIREMENTS
import com.example.gadgetmover.model.filter.SwitchCatalog
import com.example.gadgetmover.model.filter.SwitchRequirement
import com.example.gadgetmover.model.filter.summaryText
import java.util.UUID

/**
 * `FilterType.SwitchSystemBuilder` — the keyboard equivalent of [CameraSystemBuilderField]: a
 * repeatable "Add Switch" card list instead of a single flat switch-model field, so a keyboard
 * built with more than one switch (e.g. stock switches plus a hot-swapped set) can describe each
 * one separately. Each card picks a switch brand, then a model narrowed to that brand — the same
 * two-step narrowing [PhoneFilterSchema.socModel] uses for SoC Brand/Model — and, only when
 * [isListing] is true, an exact quantity. A buyer's search-filter use of this same field skips the
 * quantity control entirely: "keyboard includes this switch" is a real filter criterion, "exactly
 * how many" only matters once you're describing one specific keyboard for sale.
 */
@Composable
fun SwitchSystemBuilderField(
    requirements: List<SwitchRequirement>,
    onChange: (List<SwitchRequirement>) -> Unit,
    isListing: Boolean = false
) {
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    if (requirements.isEmpty()) {
        Text(
            "No switches added.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            requirements.forEachIndexed { index, requirement ->
                SwitchRequirementCard(
                    index = index,
                    requirement = requirement,
                    expanded = requirement.id in expandedIds,
                    isListing = isListing,
                    onToggleExpand = {
                        expandedIds = if (requirement.id in expandedIds) expandedIds - requirement.id else expandedIds + requirement.id
                    },
                    onChange = { updated -> onChange(requirements.map { if (it.id == updated.id) updated else it }) },
                    onDelete = {
                        expandedIds = expandedIds - requirement.id
                        onChange(requirements.filterNot { it.id == requirement.id })
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (requirements.size < MAX_SWITCH_REQUIREMENTS) {
        OutlinedButton(
            onClick = {
                val added = SwitchRequirement(id = UUID.randomUUID().toString())
                expandedIds = expandedIds + added.id
                onChange(requirements + added)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Switch")
        }
    }
}

@Composable
private fun SwitchRequirementCard(
    index: Int,
    requirement: SwitchRequirement,
    expanded: Boolean,
    isListing: Boolean,
    onToggleExpand: () -> Unit,
    onChange: (SwitchRequirement) -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleExpand)
                ) {
                    Text("Switch ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (!expanded) {
                        Text(
                            requirement.summaryText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Switch ${index + 1}", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse Switch ${index + 1}" else "Expand Switch ${index + 1}"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                CardFieldLabel("Switch Brand")
                SearchablePopupSelectField(
                    fieldLabel = "Switch Brand",
                    options = SwitchCatalog.brands,
                    isMultiSelect = false,
                    allowCustomInput = true,
                    selectedIds = requirement.brandId?.let { setOf(it) } ?: emptySet(),
                    onChange = { ids ->
                        // A brand change resets the model — a model picked under the old brand may not exist under the new one.
                        onChange(requirement.copy(brandId = ids.firstOrNull(), modelId = null))
                    }
                )

                val brandId = requirement.brandId
                if (brandId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Switch Model")
                    val modelOptions = if (FilterFieldValue.isCustomId(brandId)) emptyList() else SwitchCatalog.modelsFor(brandId)
                    SearchablePopupSelectField(
                        fieldLabel = "Switch Model",
                        options = modelOptions,
                        isMultiSelect = false,
                        allowCustomInput = true,
                        selectedIds = requirement.modelId?.let { setOf(it) } ?: emptySet(),
                        onChange = { ids -> onChange(requirement.copy(modelId = ids.firstOrNull())) }
                    )
                }

                if (isListing && requirement.modelId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Quantity")
                    NumberInputFieldWidget(
                        placeholder = "e.g. 90",
                        unit = "pcs",
                        text = requirement.quantity,
                        onChange = { onChange(requirement.copy(quantity = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onToggleExpand) { Text("Confirm") }
            }
        }
    }
}

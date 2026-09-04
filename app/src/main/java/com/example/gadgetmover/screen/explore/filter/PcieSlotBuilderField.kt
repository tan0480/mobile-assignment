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
import com.example.gadgetmover.model.filter.FilterOption
import com.example.gadgetmover.model.filter.MAX_PCIE_SLOT_REQUIREMENTS
import com.example.gadgetmover.model.filter.PcieGeneration
import com.example.gadgetmover.model.filter.PcieLaneWidth
import com.example.gadgetmover.model.filter.PcieSlotRequirement
import com.example.gadgetmover.model.filter.summaryText
import java.util.UUID

/**
 * `FilterType.PcieSlotBuilder` — same repeatable "Add X" card pattern as [CameraSystemBuilderField]:
 * pick a lane width, pick a generation, Confirm, then "+ Add PCIe Slot" for another. Multiple cards
 * are ANDed together (see `PcieSlotMatching`) — a board must have all of them, not just one.
 */
@Composable
fun PcieSlotBuilderField(
    requirements: List<PcieSlotRequirement>,
    onChange: (List<PcieSlotRequirement>) -> Unit
) {
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    if (requirements.isEmpty()) {
        Text(
            "No PCIe slot requirements added.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            requirements.forEachIndexed { index, requirement ->
                PcieSlotRequirementCard(
                    index = index,
                    requirement = requirement,
                    expanded = requirement.id in expandedIds,
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

    if (requirements.size < MAX_PCIE_SLOT_REQUIREMENTS) {
        OutlinedButton(
            onClick = {
                val added = PcieSlotRequirement(id = UUID.randomUUID().toString())
                expandedIds = expandedIds + added.id
                onChange(requirements + added)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add PCIe Slot")
        }
    }
}

@Composable
private fun PcieSlotRequirementCard(
    index: Int,
    requirement: PcieSlotRequirement,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onChange: (PcieSlotRequirement) -> Unit,
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
                    Text("PCIe Slot ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.Filled.Delete, contentDescription = "Delete PCIe Slot ${index + 1}", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse PCIe Slot ${index + 1}" else "Expand PCIe Slot ${index + 1}"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                CardFieldLabel("Lane Width")
                ChipGroupField(
                    options = PcieLaneWidth.entries.map { FilterOption(id = it.name, label = it.label) },
                    isMultiSelect = false,
                    selectedIds = setOf(requirement.laneWidth.name),
                    onChange = { ids ->
                        ids.firstOrNull()
                            ?.let { id -> PcieLaneWidth.entries.find { it.name == id } }
                            ?.let { onChange(requirement.copy(laneWidth = it)) }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                CardFieldLabel("Generation")
                ChipGroupField(
                    options = PcieGeneration.entries.map { FilterOption(id = it.name, label = it.label) },
                    isMultiSelect = false,
                    selectedIds = setOf(requirement.generation.name),
                    onChange = { ids ->
                        ids.firstOrNull()
                            ?.let { id -> PcieGeneration.entries.find { it.name == id } }
                            ?.let { onChange(requirement.copy(generation = it)) }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onToggleExpand) { Text("Confirm") }
            }
        }
    }
}

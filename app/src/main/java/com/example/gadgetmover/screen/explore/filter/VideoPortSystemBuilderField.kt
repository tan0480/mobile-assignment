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
import com.example.gadgetmover.model.filter.MAX_VIDEO_PORT_REQUIREMENTS
import com.example.gadgetmover.model.filter.VideoPortCatalog
import com.example.gadgetmover.model.filter.VideoPortRequirement
import com.example.gadgetmover.model.filter.summaryText
import java.util.UUID

/**
 * `FilterType.VideoPortBuilder` — the single-catalogue counterpart to [SwitchSystemBuilderField]:
 * a repeatable "Add Port" card list instead of one fixed field per port generation, so a monitor's
 * exact port mix (e.g. 2× HDMI 2.1 + 1× DisplayPort 1.4a + 1× USB-C DP Alt Mode) can be described
 * without a dedicated field per combination. Each card picks a port type from [VideoPortCatalog]
 * and, only when [isListing] is true and a type has been picked, an exact quantity — a buyer's
 * search-filter use of this field skips the quantity control entirely: "monitor has this port" is
 * a real filter criterion, "exactly how many" only matters once you're describing one specific
 * monitor for sale.
 */
@Composable
fun VideoPortSystemBuilderField(
    requirements: List<VideoPortRequirement>,
    onChange: (List<VideoPortRequirement>) -> Unit,
    isListing: Boolean = false
) {
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    if (requirements.isEmpty()) {
        Text(
            "No ports added.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            requirements.forEachIndexed { index, requirement ->
                VideoPortRequirementCard(
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

    if (requirements.size < MAX_VIDEO_PORT_REQUIREMENTS) {
        OutlinedButton(
            onClick = {
                val added = VideoPortRequirement(id = UUID.randomUUID().toString())
                expandedIds = expandedIds + added.id
                onChange(requirements + added)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) // TODO: swap with custom ImageVector
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Port")
        }
    }
}

@Composable
private fun VideoPortRequirementCard(
    index: Int,
    requirement: VideoPortRequirement,
    expanded: Boolean,
    isListing: Boolean,
    onToggleExpand: () -> Unit,
    onChange: (VideoPortRequirement) -> Unit,
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
                    Text("Port ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Port ${index + 1}", tint = MaterialTheme.colorScheme.error) // TODO: swap with custom ImageVector
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, // TODO: swap with custom ImageVector
                        contentDescription = if (expanded) "Collapse Port ${index + 1}" else "Expand Port ${index + 1}"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                CardFieldLabel("Port Type")
                SearchablePopupSelectField(
                    fieldLabel = "Port Type",
                    options = VideoPortCatalog.portTypes,
                    isMultiSelect = false,
                    allowCustomInput = true,
                    selectedIds = requirement.portTypeId?.let { setOf(it) } ?: emptySet(),
                    onChange = { ids -> onChange(requirement.copy(portTypeId = ids.firstOrNull())) }
                )

                if (isListing && requirement.portTypeId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Quantity")
                    NumberInputFieldWidget(
                        placeholder = "e.g. 2",
                        unit = "ports",
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

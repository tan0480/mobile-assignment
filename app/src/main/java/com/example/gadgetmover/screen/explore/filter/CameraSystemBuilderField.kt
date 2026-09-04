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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.model.filter.CameraRequirement
import com.example.gadgetmover.model.filter.CameraRequirementField
import com.example.gadgetmover.model.filter.CameraRole
import com.example.gadgetmover.model.filter.CameraSensorSizeOptions
import com.example.gadgetmover.model.filter.FilterOption
import com.example.gadgetmover.model.filter.MAX_CAMERA_REQUIREMENTS
import com.example.gadgetmover.model.filter.NumericRequirementMode
import com.example.gadgetmover.model.filter.summaryText
import com.example.gadgetmover.model.filter.visibleFields
import java.util.UUID

/**
 * `FilterType.CameraSystemBuilder` — a repeatable "Add Camera" requirement list instead of a
 * fixed field set, so a search can ask for e.g. a 50MP+ main camera AND a 40MP+ ultra-wide AND a
 * 5x+ periscope telephoto without picking a predefined camera count first. Each card is one
 * independent [CameraRequirement]; see that type for what it stores.
 */
@Composable
fun CameraSystemBuilderField(
    requirements: List<CameraRequirement>,
    onChange: (List<CameraRequirement>) -> Unit
) {
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    if (requirements.isEmpty()) {
        Text(
            "No camera requirements added.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            requirements.forEachIndexed { index, requirement ->
                CameraRequirementCard(
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

    if (requirements.size < MAX_CAMERA_REQUIREMENTS) {
        OutlinedButton(
            onClick = {
                val added = CameraRequirement(id = UUID.randomUUID().toString())
                expandedIds = expandedIds + added.id
                onChange(requirements + added)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) // TODO: swap with custom ImageVector
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Camera")
        }
    }
}

@Composable
private fun CameraRequirementCard(
    index: Int,
    requirement: CameraRequirement,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onChange: (CameraRequirement) -> Unit,
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
                    Text("Camera ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Camera ${index + 1}", tint = MaterialTheme.colorScheme.error) // TODO: swap with custom ImageVector
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, // TODO: swap with custom ImageVector
                        contentDescription = if (expanded) "Collapse Camera ${index + 1}" else "Expand Camera ${index + 1}"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                CardFieldLabel("Camera Role")
                ChipGroupField(
                    options = CameraRole.entries.map { FilterOption(id = it.name, label = it.label) },
                    isMultiSelect = false,
                    selectedIds = setOf(requirement.role.name),
                    onChange = { ids ->
                        val newRole = ids.firstOrNull()?.let { id -> CameraRole.entries.find { role -> role.name == id } } ?: requirement.role
                        // A role change resets every other field — a Telephoto's zoom value shouldn't silently carry over into a Main camera slot.
                        onChange(CameraRequirement(id = requirement.id, role = newRole))
                    }
                )

                val visible = requirement.role.visibleFields()

                if (CameraRequirementField.RESOLUTION in visible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Resolution")
                    RangeableNumericField(
                        mode = requirement.resolutionMode,
                        onModeChange = { onChange(requirement.copy(resolutionMode = it)) },
                        exact = requirement.resolutionExactMp, onExactChange = { onChange(requirement.copy(resolutionExactMp = it)) },
                        min = requirement.resolutionMinMp, onMinChange = { onChange(requirement.copy(resolutionMinMp = it)) },
                        max = requirement.resolutionMaxMp, onMaxChange = { onChange(requirement.copy(resolutionMaxMp = it)) },
                        onRangeChange = { newMin, newMax -> onChange(requirement.copy(resolutionMinMp = newMin, resolutionMaxMp = newMax)) },
                        unit = " MP",
                        sliderRange = 0f..200f,
                        sliderDecimals = 0
                    )
                }

                if (CameraRequirementField.SENSOR_SIZE in visible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Sensor Size")
                    RangeableSensorSizeField(
                        mode = requirement.sensorSizeMode,
                        onModeChange = { onChange(requirement.copy(sensorSizeMode = it)) },
                        exactId = requirement.sensorSizeExactId, onExactChange = { onChange(requirement.copy(sensorSizeExactId = it)) },
                        minId = requirement.sensorSizeMinId, onMinChange = { onChange(requirement.copy(sensorSizeMinId = it)) },
                        maxId = requirement.sensorSizeMaxId, onMaxChange = { onChange(requirement.copy(sensorSizeMaxId = it)) }
                    )
                }

                if (CameraRequirementField.APERTURE in visible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Aperture")
                    RangeableNumericField(
                        mode = requirement.apertureMode,
                        onModeChange = { onChange(requirement.copy(apertureMode = it)) },
                        exact = requirement.apertureExactF, onExactChange = { onChange(requirement.copy(apertureExactF = it)) },
                        min = requirement.apertureMinF, onMinChange = { onChange(requirement.copy(apertureMinF = it)) },
                        max = requirement.apertureMaxF, onMaxChange = { onChange(requirement.copy(apertureMaxF = it)) },
                        onRangeChange = { newMin, newMax -> onChange(requirement.copy(apertureMinF = newMin, apertureMaxF = newMax)) },
                        unit = "", prefix = "f/",
                        sliderRange = 0.6f..8.0f,
                        sliderDecimals = 2
                    )
                }

                if (CameraRequirementField.FOV in visible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Field of View (minimum)")
                    DecimalInputField(
                        placeholder = "e.g. 120",
                        unit = "°",
                        text = requirement.fovMinDegrees,
                        onChange = { onChange(requirement.copy(fovMinDegrees = it)) }
                    )
                }

                if (CameraRequirementField.OPTICAL_ZOOM in visible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Optical Zoom")
                    RangeableNumericField(
                        mode = requirement.zoomMode,
                        onModeChange = { onChange(requirement.copy(zoomMode = it)) },
                        exact = requirement.zoomExact, onExactChange = { onChange(requirement.copy(zoomExact = it)) },
                        min = requirement.zoomMin, onMinChange = { onChange(requirement.copy(zoomMin = it)) },
                        max = requirement.zoomMax, onMaxChange = { onChange(requirement.copy(zoomMax = it)) },
                        onRangeChange = { newMin, newMax -> onChange(requirement.copy(zoomMin = newMin, zoomMax = newMax)) },
                        unit = "×",
                        sliderRange = 1f..100f,
                        sliderDecimals = 1
                    )
                }

                if (CameraRequirementField.OIS in visible || CameraRequirementField.EIS in visible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardFieldLabel("Stabilization")
                    val stabilizationOptions = listOfNotNull(
                        FilterOption("ois", "OIS").takeIf { CameraRequirementField.OIS in visible },
                        FilterOption("eis", "EIS").takeIf { CameraRequirementField.EIS in visible }
                    )
                    val selected = buildSet {
                        if (requirement.oisRequired) add("ois")
                        if (requirement.eisRequired) add("eis")
                    }
                    CheckboxListField(
                        options = stabilizationOptions,
                        selectedIds = selected,
                        onChange = { ids ->
                            onChange(requirement.copy(oisRequired = "ois" in ids, eisRequired = "eis" in ids))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onToggleExpand) { Text("Confirm") }
            }
        }
    }
}

@Composable
fun CardFieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/** A mode selector (Exact/Minimum/Range) followed by a text field (Exact/Minimum) or a single two-thumb [RangeSlider] bar (Range) — shared by every numeric camera spec (Resolution, Aperture, Optical Zoom). */
@Composable
private fun RangeableNumericField(
    mode: NumericRequirementMode,
    onModeChange: (NumericRequirementMode) -> Unit,
    exact: String, onExactChange: (String) -> Unit,
    min: String, onMinChange: (String) -> Unit,
    max: String, onMaxChange: (String) -> Unit,
    /**
     * Commits both bounds from a single [RangeSlider] drag event in one call — the drag can't go
     * through [onMinChange]+[onMaxChange] as two separate calls, since both closures capture the
     * same pre-drag [CameraRequirement] snapshot and nothing recomposes between them, so the
     * second call's `.copy()` would silently overwrite the first (only the max bound would ever
     * stick). Defaults to that broken two-call sequence for compatibility, but every call site
     * below overrides it with a proper single-`.copy()` update.
     */
    onRangeChange: (min: String, max: String) -> Unit = { newMin, newMax -> onMinChange(newMin); onMaxChange(newMax) },
    unit: String,
    prefix: String = "",
    sliderRange: ClosedFloatingPointRange<Float>,
    sliderDecimals: Int
) {
    ChipGroupField(
        options = NumericRequirementMode.entries.map { FilterOption(id = it.name, label = it.label) },
        isMultiSelect = false,
        selectedIds = setOf(mode.name),
        onChange = { ids ->
            ids.firstOrNull()?.let { id -> NumericRequirementMode.entries.find { it.name == id } }?.let(onModeChange)
        }
    )
    Spacer(modifier = Modifier.height(8.dp))
    when (mode) {
        NumericRequirementMode.EXACT -> DecimalInputField(placeholder = "Value", unit = unit, prefix = prefix, text = exact, onChange = onExactChange)
        NumericRequirementMode.MINIMUM -> DecimalInputField(placeholder = "Minimum", unit = unit, prefix = prefix, text = min, onChange = onMinChange)
        NumericRequirementMode.RANGE -> {
            fun format(value: Float): String = "%.${sliderDecimals}f".format(value)
            val current = (min.toFloatOrNull() ?: sliderRange.start)..(max.toFloatOrNull() ?: sliderRange.endInclusive)
            Text(
                "$prefix${format(current.start)}$unit - $prefix${format(current.endInclusive)}$unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RangeSlider(
                value = current,
                onValueChange = { newRange -> onRangeChange(format(newRange.start), format(newRange.endInclusive)) },
                valueRange = sliderRange
            )
        }
    }
}

/** Same Exact/Minimum/Range shape as [RangeableNumericField], but picking from [CameraSensorSizeOptions]'s discrete catalogue instead of typing a number. */
@Composable
private fun RangeableSensorSizeField(
    mode: NumericRequirementMode,
    onModeChange: (NumericRequirementMode) -> Unit,
    exactId: String?, onExactChange: (String?) -> Unit,
    minId: String?, onMinChange: (String?) -> Unit,
    maxId: String?, onMaxChange: (String?) -> Unit
) {
    ChipGroupField(
        options = NumericRequirementMode.entries.map { FilterOption(id = it.name, label = it.label) },
        isMultiSelect = false,
        selectedIds = setOf(mode.name),
        onChange = { ids ->
            ids.firstOrNull()?.let { id -> NumericRequirementMode.entries.find { it.name == id } }?.let(onModeChange)
        }
    )
    Spacer(modifier = Modifier.height(8.dp))
    when (mode) {
        NumericRequirementMode.EXACT -> ChipGroupField(
            options = CameraSensorSizeOptions.all,
            isMultiSelect = false,
            selectedIds = exactId?.let { setOf(it) } ?: emptySet(),
            onChange = { ids -> onExactChange(ids.firstOrNull()) }
        )
        NumericRequirementMode.MINIMUM -> ChipGroupField(
            options = CameraSensorSizeOptions.all,
            isMultiSelect = false,
            selectedIds = minId?.let { setOf(it) } ?: emptySet(),
            onChange = { ids -> onMinChange(ids.firstOrNull()) }
        )
        NumericRequirementMode.RANGE -> Column {
            CardFieldLabel("From (minimum)")
            ChipGroupField(
                options = CameraSensorSizeOptions.all,
                isMultiSelect = false,
                selectedIds = minId?.let { setOf(it) } ?: emptySet(),
                onChange = { ids -> onMinChange(ids.firstOrNull()) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            CardFieldLabel("To (maximum)")
            ChipGroupField(
                options = CameraSensorSizeOptions.all,
                isMultiSelect = false,
                selectedIds = maxId?.let { setOf(it) } ?: emptySet(),
                onChange = { ids -> onMaxChange(ids.firstOrNull()) }
            )
        }
    }
}

/** Like [NumberInputFieldWidget] but allows one decimal point — camera specs like aperture (f/1.8) and optical zoom (4.3×) aren't whole numbers. */
@Composable
private fun DecimalInputField(placeholder: String, unit: String, text: String, prefix: String = "", onChange: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = { input -> if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) onChange(input) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        prefix = if (prefix.isNotEmpty()) ({ Text(prefix) }) else null,
        suffix = if (unit.isNotEmpty()) ({ Text(unit) }) else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

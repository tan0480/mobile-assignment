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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.FilterField
import com.example.gadgetmover.model.filter.FilterFieldValue
import com.example.gadgetmover.model.filter.FilterOption
import com.example.gadgetmover.model.filter.FilterType
import com.example.gadgetmover.model.filter.effectiveOptions
import com.example.gadgetmover.model.filter.formatRangeBound
import com.example.gadgetmover.ui.theme.BrandOrange

/**
 * Renders one [FilterField] with whichever widget matches its [FilterField.type], reading and
 * writing a single [FilterFieldValue] via [value]/[onValueChange]. This is the one dispatch
 * point a filter sheet needs — it never has to know which of the seven widget kinds a given
 * field uses. [state] is the whole draft, threaded through only so [FilterField.optionsForState]
 * fields (e.g. SoC Model narrowed by SoC Brand) can compute their live option list.
 *
 * When [isListing] is true, [FilterType.NumberRange] fields are rendered as a single number
 * input (the seller enters one exact value, not a range) and store a [FilterFieldValue.NumberInput]
 * instead of [FilterFieldValue.RangeInput].
 */
@Composable
fun DynamicFilterField(
    field: FilterField,
    value: FilterFieldValue?,
    state: CategoryFilterState,
    onValueChange: (FilterFieldValue) -> Unit,
    isListing: Boolean = false
) {
    val effectiveOptions = field.effectiveOptions(state)
    FilterSectionTitle(field.label)
    when (val type = field.type) {
        is FilterType.ChipGroup -> ChipGroupField(
            options = effectiveOptions,
            isMultiSelect = type.isMultiSelect,
            selectedIds = (value as? FilterFieldValue.MultiSelect)?.selectedIds
                ?: (value as? FilterFieldValue.SingleSelect)?.selectedId?.let { setOf(it) }
                ?: emptySet(),
            onChange = { ids ->
                onValueChange(
                    if (type.isMultiSelect) FilterFieldValue.MultiSelect(ids)
                    else FilterFieldValue.SingleSelect(ids.firstOrNull())
                )
            }
        )

        is FilterType.SearchablePopupSelect -> SearchablePopupSelectField(
            fieldLabel = field.label,
            options = effectiveOptions,
            isMultiSelect = type.isMultiSelect,
            allowCustomInput = type.allowCustomInput,
            selectedIds = (value as? FilterFieldValue.MultiSelect)?.selectedIds
                ?: (value as? FilterFieldValue.SingleSelect)?.selectedId?.let { setOf(it) }
                ?: emptySet(),
            onChange = { ids ->
                onValueChange(
                    if (type.isMultiSelect) FilterFieldValue.MultiSelect(ids)
                    else FilterFieldValue.SingleSelect(ids.firstOrNull())
                )
            }
        )

        FilterType.CheckboxList -> CheckboxListField(
            options = effectiveOptions,
            selectedIds = (value as? FilterFieldValue.MultiSelect)?.selectedIds ?: emptySet(),
            onChange = { onValueChange(FilterFieldValue.MultiSelect(it)) }
        )

        FilterType.RadioGroup -> RadioGroupField(
            options = effectiveOptions,
            selectedId = (value as? FilterFieldValue.SingleSelect)?.selectedId,
            onChange = { onValueChange(FilterFieldValue.SingleSelect(it)) }
        )

        is FilterType.NumberInputField -> NumberInputFieldWidget(
            placeholder = type.placeholder,
            unit = type.unit,
            text = (value as? FilterFieldValue.NumberInput)?.value ?: "",
            onChange = { onValueChange(FilterFieldValue.NumberInput(it)) }
        )

        is FilterType.NumberRange -> {
            if (isListing) {
                val unit = type.unit.trim()
                val text = (value as? FilterFieldValue.NumberInput)?.value ?: ""
                val entered = text.toFloatOrNull()
                // The min/max shown in the placeholder is a worked example, not a hard limit — a seller's actual
                // item can reasonably fall outside it — but a value must still be a real, sane number: greater
                // than 0 and under 10x the example's upper bound, so a typo like an extra zero gets caught.
                val upperBound = type.max * 10
                val outOfBounds = entered != null && (entered <= 0f || entered >= upperBound)
                NumberInputFieldWidget(
                    placeholder = "e.g. ${cleanNumberText(type.min)}-${cleanNumberText(type.max)}",
                    unit = unit,
                    text = text,
                    onChange = { onValueChange(FilterFieldValue.NumberInput(it)) },
                    isError = outOfBounds,
                    supportingText = if (outOfBounds) "Enter a value greater than 0 and under ${cleanNumberText(upperBound)}$unit" else null,
                    // Plenty of specs are fractional (screen size, thickness, sensitivity, driver size, ...) —
                    // digits-only would silently block the decimal point for every one of them.
                    allowDecimal = true
                )
            } else {
                NumberRangeField(
                    type = type,
                    range = (value as? FilterFieldValue.RangeInput)?.range ?: (type.min..type.max),
                    onChange = { onValueChange(FilterFieldValue.RangeInput(it)) }
                )
            }
        }

        is FilterType.NumberRangeWithUnitToggle -> {
            if (isListing) {
                val current = value as? FilterFieldValue.UnitNumberInput
                val unit = current?.unit?.takeIf { it.isNotEmpty() } ?: type.units.first().unit
                val active = type.units.find { it.unit == unit } ?: type.units.first()
                val text = current?.value ?: ""
                val entered = text.toFloatOrNull()
                val upperBound = active.max * 10
                val outOfBounds = entered != null && (entered <= 0f || entered >= upperBound)
                Column {
                    UnitToggleRow(units = type.units, selectedUnit = unit, onSelect = { newUnit ->
                        onValueChange(FilterFieldValue.UnitNumberInput(newUnit, text))
                    })
                    NumberInputFieldWidget(
                        placeholder = "e.g. ${cleanNumberText(active.min)}-${cleanNumberText(active.max)}",
                        unit = active.unit,
                        text = text,
                        onChange = { onValueChange(FilterFieldValue.UnitNumberInput(unit, it)) },
                        isError = outOfBounds,
                        supportingText = if (outOfBounds) "Enter a value greater than 0 and under ${cleanNumberText(upperBound)}${active.unit}" else null,
                        allowDecimal = true
                    )
                }
            } else {
                val current = value as? FilterFieldValue.UnitRangeInput
                val unit = current?.unit?.takeIf { it.isNotEmpty() } ?: type.units.first().unit
                val active = type.units.find { it.unit == unit } ?: type.units.first()
                val range = current?.range ?: (active.min..active.max)
                UnitNumberRangeField(
                    type = type,
                    unit = unit,
                    range = range,
                    onChange = { newUnit, newRange -> onValueChange(FilterFieldValue.UnitRangeInput(newUnit, newRange)) }
                )
            }
        }

        is FilterType.SwitchToggle -> SwitchToggleField(
            label = type.label,
            enabled = (value as? FilterFieldValue.Toggle)?.enabled ?: false,
            onChange = { onValueChange(FilterFieldValue.Toggle(it)) }
        )

        FilterType.CameraSystemBuilder -> CameraSystemBuilderField(
            requirements = (value as? FilterFieldValue.CameraRequirements)?.items ?: emptyList(),
            onChange = { onValueChange(FilterFieldValue.CameraRequirements(it)) },
            isListing = isListing
        )

        FilterType.PcieSlotBuilder -> PcieSlotBuilderField(
            requirements = (value as? FilterFieldValue.PcieSlotRequirements)?.items ?: emptyList(),
            onChange = { onValueChange(FilterFieldValue.PcieSlotRequirements(it)) }
        )

        FilterType.SwitchSystemBuilder -> SwitchSystemBuilderField(
            requirements = (value as? FilterFieldValue.SwitchRequirements)?.items ?: emptyList(),
            onChange = { onValueChange(FilterFieldValue.SwitchRequirements(it)) },
            isListing = isListing
        )

        FilterType.VideoPortBuilder -> VideoPortSystemBuilderField(
            requirements = (value as? FilterFieldValue.VideoPortRequirements)?.items ?: emptyList(),
            onChange = { onValueChange(FilterFieldValue.VideoPortRequirements(it)) },
            isListing = isListing
        )
    }
}

@Composable
fun FilterSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

/**
 * Lays [options] out as a grid of cells that are all the same size — every cell in a row shares
 * that row's height (the tallest label decides it) and every column shares an equal width, same
 * as a typical marketplace filter grid. A plain [FlowRow] can't do this: it packs each chip to
 * its own natural width, so rows end up ragged.
 */
@Composable
private fun EqualSizeOptionGrid(
    options: List<FilterOption>,
    columns: Int,
    cell: @Composable (FilterOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { option ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        cell(option)
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OptionCell(label: String, isSelected: Boolean, alignStart: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isSelected) BrandOrange else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            textAlign = if (alignStart) TextAlign.Start else TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Long labels get cramped when forced into multiple columns, so they fall back to one full-width
 * row each. A single unbreakable word (e.g. "Hasselblad") is just as much a problem as a long
 * overall label — a 2-column cell can be too narrow for it even when the whole label is short,
 * forcing Compose to wrap mid-word — so this checks the longest individual word, not just the
 * longest full label.
 */
private fun optionGridColumns(options: List<FilterOption>): Int {
    val longestLabel = options.maxOfOrNull { it.label.length } ?: 0
    val longestWord = options.flatMap { it.label.split(" ") }.maxOfOrNull { it.length } ?: 0
    return if (longestLabel > 18 || longestWord > 9) 1 else 2
}

/** `FilterType.ChipGroup` — a finite set of discrete options, single- or multi-select. */
@Composable
fun ChipGroupField(
    options: List<FilterOption>,
    isMultiSelect: Boolean,
    selectedIds: Set<String>,
    onChange: (Set<String>) -> Unit,
    columns: Int = optionGridColumns(options)
) {
    EqualSizeOptionGrid(options, columns) { option ->
        val isSelected = selectedIds.contains(option.id)
        OptionCell(
            label = option.label,
            isSelected = isSelected,
            alignStart = columns == 1,
            onClick = {
                onChange(
                    when {
                        isMultiSelect && isSelected -> selectedIds - option.id
                        isMultiSelect -> selectedIds + option.id
                        isSelected -> emptySet()
                        else -> setOf(option.id)
                    }
                )
            }
        )
    }
}

/** `FilterType.CheckboxList` — independently toggleable features, shown as the same equal-size grid. */
@Composable
fun CheckboxListField(
    options: List<FilterOption>,
    selectedIds: Set<String>,
    onChange: (Set<String>) -> Unit,
    columns: Int = optionGridColumns(options)
) {
    EqualSizeOptionGrid(options, columns) { option ->
        val isSelected = selectedIds.contains(option.id)
        OptionCell(
            label = option.label,
            alignStart = columns == 1,
            isSelected = isSelected,
            onClick = { onChange(if (isSelected) selectedIds - option.id else selectedIds + option.id) }
        )
    }
}

/** `FilterType.RadioGroup` — strictly mutually exclusive options, shown as the same equal-size grid. */
@Composable
fun RadioGroupField(
    options: List<FilterOption>,
    selectedId: String?,
    onChange: (String?) -> Unit,
    columns: Int = optionGridColumns(options)
) {
    EqualSizeOptionGrid(options, columns) { option ->
        OptionCell(
            label = option.label,
            isSelected = selectedId == option.id,
            alignStart = columns == 1,
            onClick = { onChange(option.id) }
        )
    }
}

/** `FilterType.NumberInputField` — a single precise numeric input, e.g. battery capacity. */
@Composable
fun NumberInputFieldWidget(
    placeholder: String,
    unit: String,
    text: String,
    onChange: (String) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    /** Whether a decimal point is accepted — off for whole-count specs (e.g. a switch quantity), on for anything measured (screen size, thickness, sensitivity, ...) where forcing a whole number would just be wrong. */
    allowDecimal: Boolean = false
) {
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val isValid = if (allowDecimal) {
                input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }
            } else {
                input.all { it.isDigit() }
            }
            if (isValid) onChange(input)
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        suffix = { Text(unit) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number)
    )
}

/** Formats [value] without a trailing ".0" for whole numbers (e.g. "8" not "8.0"), but keeps real fractional precision (e.g. "5.4") — used for placeholder/bound text where the field's own [FilterType.NumberRange.min]/[max] may or may not be a whole number. */
private fun cleanNumberText(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()

/**
 * `FilterType.NumberRange` — a continuous dual-ended range, e.g. price or capacity bounds. The
 * slider itself can only ever land within [FilterType.NumberRange.min]/[max], but a buyer who
 * genuinely wants something outside that (e.g. "> 64GB" RAM, "< 500W" PSU) can still type it via
 * the custom-range inputs below — those aren't clamped, so [range] itself can carry a value past
 * either bound; the slider just displays its own coerced view of it in that case.
 */
@Composable
fun NumberRangeField(
    type: FilterType.NumberRange,
    range: ClosedFloatingPointRange<Float>,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val steps = if (type.step > 0f) (((type.max - type.min) / type.step).toInt() - 1).coerceAtLeast(0) else 0
    val isCustomBelow = range.start < type.min
    val isCustomAbove = range.endInclusive > type.max
    Text(
        "${formatRangeBound(type, range.start)}${if (isCustomBelow) " (custom)" else ""}" +
            " - ${formatRangeBound(type, range.endInclusive)}${if (isCustomAbove) " (custom)" else ""}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    RangeSlider(
        value = range.start.coerceIn(type.min, type.max)..range.endInclusive.coerceIn(type.min, type.max),
        onValueChange = onChange,
        valueRange = type.min..type.max,
        steps = steps
    )

    var showCustomRange by remember { mutableStateOf(isCustomBelow || isCustomAbove) }
    TextButton(onClick = { showCustomRange = !showCustomRange }) {
        Text(if (showCustomRange) "Hide custom min/max" else "Outside this range? Enter a custom min or max")
    }
    if (showCustomRange) {
        var minInput by remember { mutableStateOf(if (isCustomBelow) cleanNumberText(range.start) else "") }
        var maxInput by remember { mutableStateOf(if (isCustomAbove) cleanNumberText(range.endInclusive) else "") }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = minInput,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) {
                        minInput = input
                        input.toFloatOrNull()?.let { onChange(it..range.endInclusive) }
                    }
                },
                label = { Text("Min ${type.unit.trim()}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = maxInput,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) {
                        maxInput = input
                        input.toFloatOrNull()?.let { onChange(range.start..it) }
                    }
                },
                label = { Text("Max ${type.unit.trim()}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}

/** The unit picker (e.g. "GB" / "TB") shown above a [FilterType.NumberRangeWithUnitToggle] field, in either listing or filter mode. */
@Composable
fun UnitToggleRow(units: List<FilterType.UnitRange>, selectedUnit: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        units.forEach { unitRange ->
            val selected = unitRange.unit == selectedUnit
            OutlinedButton(
                onClick = { onSelect(unitRange.unit) },
                shape = RoundedCornerShape(20.dp),
                colors = if (selected) {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = BrandOrange.copy(alpha = 0.15f),
                        contentColor = BrandOrange
                    )
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(unitRange.unit)
            }
        }
    }
}

/** `FilterType.NumberRangeWithUnitToggle` (filter mode) — a [NumberRangeField]-style dual-ended range slider, but re-bounded to whichever [FilterType.UnitRange] is currently selected via [UnitToggleRow]. */
@Composable
fun UnitNumberRangeField(
    type: FilterType.NumberRangeWithUnitToggle,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    onChange: (unit: String, range: ClosedFloatingPointRange<Float>) -> Unit
) {
    val active = type.units.find { it.unit == unit } ?: type.units.first()
    UnitToggleRow(units = type.units, selectedUnit = active.unit) { newUnit ->
        val newActive = type.units.find { it.unit == newUnit } ?: return@UnitToggleRow
        onChange(newUnit, newActive.min..newActive.max)
    }
    val steps = if (active.step > 0f) (((active.max - active.min) / active.step).toInt() - 1).coerceAtLeast(0) else 0
    Text(
        "${cleanNumberText(range.start)} ${active.unit} - ${cleanNumberText(range.endInclusive)} ${active.unit}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    RangeSlider(
        value = range,
        onValueChange = { onChange(active.unit, it) },
        valueRange = active.min..active.max,
        steps = steps
    )
}

/** `FilterType.SwitchToggle` — a single boolean flag, e.g. "Hot-swappable only". */
@Composable
fun SwitchToggleField(label: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!enabled) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

/**
 * `FilterType.SearchablePopupSelect` — shown inline as a button summarizing the current
 * selection; tapping it opens [SearchablePopupDialog] to actually search/select.
 */
@Composable
fun SearchablePopupSelectField(
    fieldLabel: String,
    options: List<FilterOption>,
    isMultiSelect: Boolean,
    allowCustomInput: Boolean,
    selectedIds: Set<String>,
    onChange: (Set<String>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val summary = remember(selectedIds, options) {
        if (selectedIds.isEmpty()) {
            "Any $fieldLabel"
        } else {
            selectedIds.joinToString(", ") { id ->
                if (FilterFieldValue.isCustomId(id)) FilterFieldValue.customLabel(id)
                else options.find { it.id == id }?.label ?: id
            }
        }
    }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                summary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
    }

    if (showDialog) {
        SearchablePopupDialog(
            title = fieldLabel,
            options = options,
            isMultiSelect = isMultiSelect,
            allowCustomInput = allowCustomInput,
            selectedIds = selectedIds,
            onDismiss = { showDialog = false },
            onConfirm = { onChange(it) }
        )
    }
}

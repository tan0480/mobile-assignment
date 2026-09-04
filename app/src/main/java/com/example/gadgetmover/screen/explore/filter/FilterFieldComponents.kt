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
                NumberInputFieldWidget(
                    placeholder = "${type.min.toInt()} – ${type.max.toInt()}",
                    unit = unit,
                    text = (value as? FilterFieldValue.NumberInput)?.value ?: "",
                    onChange = { onValueChange(FilterFieldValue.NumberInput(it)) }
                )
            } else {
                NumberRangeField(
                    type = type,
                    range = (value as? FilterFieldValue.RangeInput)?.range ?: (type.min..type.max),
                    onChange = { onValueChange(FilterFieldValue.RangeInput(it)) }
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
            onChange = { onValueChange(FilterFieldValue.CameraRequirements(it)) }
        )

        FilterType.PcieSlotBuilder -> PcieSlotBuilderField(
            requirements = (value as? FilterFieldValue.PcieSlotRequirements)?.items ?: emptyList(),
            onChange = { onValueChange(FilterFieldValue.PcieSlotRequirements(it)) }
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
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = { input -> if (input.all { it.isDigit() }) onChange(input) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        suffix = { Text(unit) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/** `FilterType.NumberRange` — a continuous dual-ended range, e.g. price or capacity bounds. */
@Composable
fun NumberRangeField(
    type: FilterType.NumberRange,
    range: ClosedFloatingPointRange<Float>,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val steps = if (type.step > 0f) (((type.max - type.min) / type.step).toInt() - 1).coerceAtLeast(0) else 0
    Text(
        "${formatRangeBound(type, range.start)} - ${formatRangeBound(type, range.endInclusive)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    RangeSlider(
        value = range,
        onValueChange = onChange,
        valueRange = type.min..type.max,
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

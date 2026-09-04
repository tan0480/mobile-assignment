package com.example.gadgetmover.model.filter

/**
 * Formats one bound of a [FilterType.NumberRange] value, e.g. `250mm` or `RM50` depending on
 * [FilterType.NumberRange.unitIsPrefix]. Shared by the filter sheet's slider label
 * (`NumberRangeField` in `screen/explore/filter/FilterFieldComponents.kt`) and [displayText]'s
 * read-only rendering below, so the two never drift.
 */
fun formatRangeBound(type: FilterType.NumberRange, value: Float): String {
    val numberText = if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
    return if (type.unitIsPrefix) "${type.unit}$numberText" else "$numberText${type.unit}"
}

private fun resolveOptionLabel(id: String, options: List<FilterOption>): String =
    if (FilterFieldValue.isCustomId(id)) FilterFieldValue.customLabel(id)
    else options.find { it.id == id }?.label ?: id

/**
 * Renders one filled-in [FilterFieldValue] as plain display text for a read-only view (product
 * detail) — the inverse of what [FilterField]'s interactive widgets in
 * `screen/explore/filter/FilterFieldComponents.kt` let a user edit. Only meaningful when
 * `value.isFilled(field)`; callers are expected to have already filtered on that.
 */
fun FilterFieldValue.displayText(field: FilterField, state: CategoryFilterState): String {
    val options = field.effectiveOptions(state)
    return when (this) {
        is FilterFieldValue.MultiSelect -> selectedIds.joinToString(", ") { resolveOptionLabel(it, options) }
        is FilterFieldValue.SingleSelect -> selectedId?.let { resolveOptionLabel(it, options) } ?: ""
        is FilterFieldValue.NumberInput -> {
            val unit = when (val ft = field.type) {
                is FilterType.NumberInputField -> ft.unit
                is FilterType.NumberRange -> ft.unit.trim()
                else -> ""
            }
            if (unit.isNotEmpty()) "$value$unit" else value
        }
        is FilterFieldValue.RangeInput -> {
            val type = field.type as? FilterType.NumberRange
            if (type == null) "${range.start} - ${range.endInclusive}"
            else "${formatRangeBound(type, range.start)} - ${formatRangeBound(type, range.endInclusive)}"
        }
        is FilterFieldValue.UnitNumberInput -> if (unit.isNotEmpty()) "$value $unit" else value
        is FilterFieldValue.UnitRangeInput -> {
            fun bound(v: Float): String {
                val numberText = if (v == v.toInt().toFloat()) v.toInt().toString() else "%.1f".format(v)
                return "$numberText $unit"
            }
            "${bound(range.start)} - ${bound(range.endInclusive)}"
        }
        is FilterFieldValue.Toggle -> "Yes"
        is FilterFieldValue.CameraRequirements -> items.joinToString("; ") { it.summaryText() }
        is FilterFieldValue.PcieSlotRequirements -> items.joinToString("; ") { it.summaryText() }
        is FilterFieldValue.SwitchRequirements -> items.joinToString("; ") { it.summaryText() }
        is FilterFieldValue.VideoPortRequirements -> items.joinToString("; ") { it.summaryText() }
    }
}

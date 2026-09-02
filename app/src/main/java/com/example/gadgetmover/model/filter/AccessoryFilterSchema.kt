package com.example.gadgetmover.model.filter

/**
 * The Accessories advanced filter schema. No dedicated spec was given for this catch-all category
 * (cases, chargers, cables, stands, adapters, etc.) — it exists mainly so `brand` (now
 * category-specific everywhere, see [CategoryFilterSchema]) still has somewhere to live for
 * Accessory listings instead of silently disappearing from the filter sheet.
 */
object AccessoryFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Anker", "Belkin", "UGREEN", "Baseus", "Spigen", "ESR", "OtterBox", "Satechi", "Logitech",
            "RAVPower", "Aukey", "Mophie", "Native Union", "Twelve South", "Peak Design", "Other"
        )
    )

    val schema = CategoryFilterSchema(sections = listOf(brand))
}

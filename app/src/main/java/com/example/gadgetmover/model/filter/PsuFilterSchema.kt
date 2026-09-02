package com.example.gadgetmover.model.filter

/** The Power Supplies (PSU) advanced filter schema. */
object PsuFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Corsair", "Seasonic", "EVGA", "be quiet!", "Cooler Master", "Thermaltake",
            "MSI", "ASUS ROG", "FSP", "Super Flower", "Antec", "Silverstone", "Other"
        )
    )

    val wattage = FilterField(
        key = "wattage",
        label = "Wattage",
        type = FilterType.NumberRange(min = 100f, max = 1600f, step = 50f, unit = "W", unitIsPrefix = false)
    )

    val efficiencyRating = FilterField(
        key = "efficiency_rating",
        label = "80 PLUS Efficiency Rating",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "80 PLUS Titanium", "80 PLUS Platinum", "80 PLUS Gold",
            "80 PLUS Silver", "80 PLUS Bronze", "80 PLUS White", "Other"
        )
    )

    val modularity = FilterField(
        key = "modularity",
        label = "Modularity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Fully Modular", "Semi-Modular", "Non-Modular", "Other")
    )

    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("ATX", "SFX", "SFX-L", "TFX", "Other")
    )

    val atxStandard = FilterField(
        key = "atx_standard",
        label = "ATX Standard",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("ATX 3.1", "ATX 3.0", "ATX 2.x", "Other")
    )

    val fanSize = FilterField(
        key = "fan_size",
        label = "Fan Size",
        type = FilterType.NumberRange(min = 80f, max = 140f, step = 10f, unit = "mm", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "All Japanese Capacitors", "12VHPWR / 12V-2x6 Connector", "Zero RPM Fan Mode (Fanless at Idle)",
            "LCD / Digital Display", "10-Year Warranty", "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, wattage, efficiencyRating, modularity,
            formFactor, atxStandard, fanSize,
            features
        )
    )
}

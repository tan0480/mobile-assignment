package com.example.gadgetmover.model.filter

/** The Case Fans advanced filter schema. */
object CaseFanFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Noctua", "Corsair", "NZXT", "Lian Li", "be quiet!", "Arctic",
            "Thermaltake", "Cooler Master", "Deepcool", "Other"
        )
    )

    val fanSize = FilterField(
        key = "fan_size",
        label = "Fan Size",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("80mm", "92mm", "120mm", "140mm", "200mm", "Other")
    )

    val bearingType = FilterField(
        key = "bearing_type",
        label = "Bearing Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Fluid Dynamic Bearing (FDB)", "Ball Bearing", "Magnetic Levitation (Maglev)", "Sleeve Bearing", "Other")
    )

    val airflow = FilterField(
        key = "airflow",
        label = "Airflow",
        type = FilterType.NumberRange(min = 10f, max = 150f, step = 5f, unit = " CFM", unitIsPrefix = false)
    )

    val staticPressure = FilterField(
        key = "static_pressure",
        label = "Static Pressure",
        type = FilterType.NumberRange(min = 0.5f, max = 5f, step = 0.1f, unit = " mmH2O", unitIsPrefix = false)
    )

    val noiseLevel = FilterField(
        key = "noise_level",
        label = "Noise Level",
        type = FilterType.NumberRange(min = 5f, max = 40f, step = 1f, unit = " dBA", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options("RGB / ARGB Lighting", "PWM Control (4-Pin)", "Daisy-Chainable", "Anti-Vibration Pads", "Other")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, fanSize, bearingType,
            airflow, staticPressure, noiseLevel,
            features
        )
    )
}

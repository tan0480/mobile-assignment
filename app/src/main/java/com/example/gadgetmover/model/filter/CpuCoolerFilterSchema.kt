package com.example.gadgetmover.model.filter

/** The CPU Coolers advanced filter schema. Socket compatibility options are shared with [CpuFilterSchema] and [MotherboardFilterSchema] via [PcSocketOptions]. */
object CpuCoolerFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    private const val AIR_COOLER_ID = "air_cooler"
    private const val AIO_ID = "aio_liquid_cooler"
    private const val CUSTOM_LOOP_ID = "custom_loop"
    private val airCoolerDependency = FieldDependency("cooler_type", setOf(AIR_COOLER_ID))
    private val liquidCoolerDependency = FieldDependency("cooler_type", setOf(AIO_ID, CUSTOM_LOOP_ID))

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Noctua", "be quiet!", "Cooler Master", "NZXT", "Corsair", "Arctic",
            "Deepcool", "Thermalright", "ID-Cooling", "Scythe", "Other"
        )
    )

    val coolerType = FilterField(
        key = "cooler_type",
        label = "Cooler Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Air Cooler", "AIO Liquid Cooler", "Custom Loop", "Other")
    )

    val radiatorSize = FilterField(
        key = "radiator_size",
        label = "Radiator Size",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("120mm", "240mm", "280mm", "360mm", "420mm", "Other"),
        visibleWhen = liquidCoolerDependency
    )

    val heatPipeCount = FilterField(
        key = "heat_pipe_count",
        label = "Heat Pipe Count",
        type = FilterType.NumberRange(min = 0f, max = 10f, step = 1f, unit = "", unitIsPrefix = false),
        visibleWhen = airCoolerDependency
    )

    val heatPipeDiameter = FilterField(
        key = "heat_pipe_diameter",
        label = "Heat Pipe Diameter",
        type = FilterType.NumberRange(min = 2f, max = 12f, step = 1f, unit = "mm", unitIsPrefix = false),
        visibleWhen = airCoolerDependency
    )

    val radiatorThickness = FilterField(
        key = "radiator_thickness",
        label = "Radiator Thickness",
        type = FilterType.NumberRange(min = 10f, max = 50f, step = 1f, unit = "mm", unitIsPrefix = false),
        visibleWhen = liquidCoolerDependency
    )

    val warrantyYears = FilterField(
        key = "warranty_years",
        label = "Warranty Period",
        type = FilterType.NumberRange(min = 1f, max = 10f, step = 1f, unit = " Years", unitIsPrefix = false),
        visibleWhen = liquidCoolerDependency
    )

    val leakDamageCoverage = FilterField(
        key = "leak_damage_coverage",
        label = "Full System Leak Damage Coverage",
        type = FilterType.SwitchToggle(label = "Includes full system leak damage compensation"),
        visibleWhen = liquidCoolerDependency
    )

    val fanSize = FilterField(
        key = "fan_size",
        label = "Fan Size",
        type = FilterType.NumberRange(min = 80f, max = 140f, step = 10f, unit = "mm", unitIsPrefix = false)
    )

    val socketCompatibility = FilterField(
        key = "socket_compatibility",
        label = "Socket Compatibility",
        type = FilterType.CheckboxList,
        options = options(*PcSocketOptions.labels)
    )

    val tdpRating = FilterField(
        key = "tdp_rating",
        label = "TDP Rating",
        type = FilterType.NumberRange(min = 65f, max = 350f, step = 5f, unit = "W", unitIsPrefix = false)
    )

    val noiseLevel = FilterField(
        key = "noise_level",
        label = "Noise Level",
        type = FilterType.NumberRange(min = 10f, max = 40f, step = 1f, unit = " dBA", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options("RGB / ARGB Lighting", "LCD Display", "Low-Profile (SFF Compatible)", "PWM Control", "Other")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, coolerType,
            radiatorSize, heatPipeCount, heatPipeDiameter,
            radiatorThickness, warrantyYears, leakDamageCoverage,
            fanSize, socketCompatibility, tdpRating, noiseLevel,
            features
        )
    )
}

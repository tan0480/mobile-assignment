package com.example.gadgetmover.model.filter

/** The Mice advanced filter schema — mirrors [KeyboardFilterSchema]'s structure but with fields specific to mice. */
object MiceFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Logitech", "Logitech G", "Razer", "SteelSeries", "Corsair", "ASUS ROG", "Glorious", "Pulsar",
            "Zowie (BenQ)", "Finalmouse", "Endgame Gear", "Lamzu", "VXE", "VGN", "Attack Shark", "G-Wolves",
            "Xtrfy", "HyperX", "Cooler Master", "ROCCAT", "Redragon", "Rapoo", "Dareu", "Fantech",
            "Pwnage", "Vaxee", "Ninjutso", "Other"
        )
    )

    val gripStyle = FilterField(
        key = "grip_style",
        label = "Grip Style",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Symmetrical", "Right-handed Ergonomic", "Left-handed Ergonomic", "Vertical / Ergonomic", "Other")
    )

    val lengthMm = FilterField(
        key = "length_mm",
        label = "Length (mm)",
        type = FilterType.NumberRange(min = 90f, max = 140f, step = 5f, unit = "mm", unitIsPrefix = false)
    )

    val widthMm = FilterField(
        key = "width_mm",
        label = "Width (mm)",
        type = FilterType.NumberRange(min = 50f, max = 85f, step = 5f, unit = "mm", unitIsPrefix = false)
    )

    val heightMm = FilterField(
        key = "height_mm",
        label = "Height (mm)",
        type = FilterType.NumberRange(min = 30f, max = 55f, step = 5f, unit = "mm", unitIsPrefix = false)
    )

    val sensorModel = FilterField(
        key = "sensor_model",
        label = "Sensor Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "PixArt PAW3950", "PixArt PAW3950 Ultra", "PixArt PAW3395", "PixArt PAW3370",
            "PixArt PMW3389", "PixArt PMW3360", "PixArt PAW3335", "PixArt PAW3311", "PixArt PAW3212",
            "Razer Focus Pro 35K Gen-2", "Razer Focus Pro 30K", "Razer Focus+ 20K",
            "Logitech HERO 2 (HERO 32K)", "Logitech HERO 25K", "Logitech HERO 16K",
            "SteelSeries TrueMove Air", "SteelSeries TrueMove Pro", "SteelSeries TrueMove 3+",
            "Other"
        )
    )

    val maxDpi = FilterField(
        key = "max_dpi",
        label = "Max DPI / CPI",
        type = FilterType.NumberRange(min = 800f, max = 42000f, step = 200f, unit = " DPI", unitIsPrefix = false)
    )

    /** Connectivity + Polling Rate — shared architecture, see [ConnectivityPollingFields]. */

    val programmableButtons = FilterField(
        key = "programmable_buttons",
        label = "Programmable Buttons",
        type = FilterType.NumberRange(min = 0f, max = 20f, step = 1f, unit = "", unitIsPrefix = false)
    )

    val switchType = FilterField(
        key = "switch_type",
        label = "Switch Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Optical Microswitch", "Mechanical Microswitch", "Silent Microswitch", "Other")
    )

    val switchModel = FilterField(
        key = "switch_model",
        label = "Switch Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Huano Blue Shell Pink Dot", "Huano Blue Shell White Dot", "Huano Transparent Blue Shell Pink Dot",
            "Huano Green Dot", "Huano Silent Brown Yellow Dot",
            "TTC Gold Dustproof 30M/60M/80M", "TTC Optical Switch", "TTC Falcon", "TTC Silent",
            "Kailh GM 8.0 (Black Mamba)", "Kailh GM 4.0 (Red)", "Kailh GM 2.0 (Blue)", "Kailh Optical", "Kailh Mute Silent",
            "Omron D2FC-F-K 50M (Blue Dot)", "Omron 20M (White Dot)", "Omron Optical Switch (D2FP-FN2)", "Omron D2F-01F (Japanese Grey Dot)",
            "Razer Gen-3 Optical", "Razer Gen-2 Optical", "Razer Gen-1 Optical",
            "Other"
        )
    )

    val scrollWheelType = FilterField(
        key = "scroll_wheel_type",
        label = "Scroll Wheel Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Standard", "Free-spin / Infinite Scroll", "Dual-mode SmartWheel", "Horizontal Scroll / Tilt Wheel", "Other")
    )

    val weight = FilterField(
        key = "weight",
        label = "Weight",
        type = FilterType.NumberRange(min = 30f, max = 150f, step = 5f, unit = "g", unitIsPrefix = false)
    )

    const val BATTERY_TYPE_BUILT_IN_ID = "built_in_lithium_ion_rechargeable"

    val batteryType = FilterField(
        key = "battery_type",
        label = "Battery Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Dry Battery / Replaceable (AA / AAA)", "Built-in Lithium-ion Rechargeable", "Wired Only (No Battery)", "Other")
    )

    val chargingMethod = FilterField(
        key = "charging_method",
        label = "Charging Method",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Type-C Wired Fast Charging", "Qi Wireless Charging", "Magnetic Charging Dock / Base", "Powerplay / Continuous Surface Charging", "Other"),
        visibleWhen = FieldDependency("battery_type", setOf(BATTERY_TYPE_BUILT_IN_ID))
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "On-board Memory",
            "RGB Backlight",
            "Honeycomb / Lightweight Shell",
            "Multi-device Pairing",
            "Hot-swappable Switch Sockets",
            "PTFE Skates / Glass Skates Included",
            "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            gripStyle, lengthMm, widthMm, heightMm,
            sensorModel, maxDpi
        ) + ConnectivityPollingFields.fields + listOf(
            programmableButtons,
            switchType, switchModel,
            scrollWheelType,
            weight,
            batteryType, chargingMethod,
            features
        )
    )
}

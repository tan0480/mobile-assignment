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
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "3Dconnexion", "8BitDo", "A4Tech", "Acer", "AG One", "Ajazz", "Alienware", "Amazon Basics",
            "Anker", "Apple", "ASRock", "ASUS", "ASUS ROG", "ATK", "Attack Shark", "AULA", "BenQ",
            "BenQ Zowie", "Bloody", "Cherry", "Cooler Master", "Corsair", "Cougar", "Dareu", "Darmoshark",
            "Delux", "Dell", "Dream Machines", "Edifier Hecate", "Elecom", "Endgame Gear", "E-YOOSO", "EVGA",
            "Fantech", "Finalmouse", "Fnatic Gear", "Fuhlen", "GameSir", "Genius", "Gigabyte",
            "Gigabyte Aorus", "Glorious", "Gravastar", "G-Wolves", "Hama", "Havit", "HP", "HyperX",
            "iBuypower", "iClever", "iMICE", "Incott", "Inphic", "InWin", "Ironcat", "JLab", "Keychron",
            "Kensington", "KeyX", "Kingston", "Klim", "Kone", "Kysona", "Lamzu", "Langtu", "Lemokey",
            "Lenovo", "Lenovo Legion", "Lofree", "Logitech", "Logitech G", "Mad Catz", "Madlions",
            "Machenike", "MageGee", "Marvo", "Matias", "MCHOSE", "Meetion", "Metapanda", "Microsoft",
            "Monka", "Monoprice", "Motospeed", "MSI", "Nacon", "Ninjutso", "Nulea", "NZXT", "Omen",
            "Patriot Viper", "Perixx", "Phylina", "Ploopy", "Pulsar", "QPAD", "Rapoo", "RAWM", "Razer",
            "Redragon", "Rexus", "Rii", "Roccat", "Rosewill", "Sades", "Samsung", "Santali", "Satechi",
            "Scyrox", "Seenda", "Sharkoon", "Sony", "Speedlink", "SteelSeries", "Swiftpoint", "Tecware",
            "Thermaltake", "Thunderobot", "Titanwolf", "Trust", "Turtle Beach", "Ugreen", "UtechSmart",
            "Varmilo", "VAXEE", "Venom", "VGN", "VXE", "Waizowl", "WLMOUSE", "Xiaomi", "Xtrfy", "Zaopin",
            "Zelotes", "Ziyoulang", "Unknown"
        )
    )

    val gripStyle = FilterField(
        key = "grip_style",
        label = "Grip Style",
        type = FilterType.ChipGroup(isMultiSelect = false),
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
            "SteelSeries TrueMove Air", "SteelSeries TrueMove Pro", "SteelSeries TrueMove 3+"
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
        type = FilterType.ChipGroup(isMultiSelect = false),
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
            "D2F-01F", "D2F-01", "D2FC-F-7N", "D2FC-F-7N 10M", "D2FC-F-7N 20M", "D2FC-F-7N 50M",
            "D2FC-F-K 50M", "D2FC-F-K 60M", "D2FP-FN2", "D2FP-FN2 60M",
            "GM 2.0 Teal", "GM 4.0 Red", "GM 8.0 Black", "GM 9.0 Burgundy", "GM 10.0 Blue", "GM 11.0 White",
            "Silent Square Red Dot", "Silent Square Yellow Dot",
            "Blue Shell Blue Dot", "Blue Shell White Dot", "Blue Shell Pink Dot",
            "Black Shell Blue Dot", "Black Shell Green Dot",
            "Transparent Blue Shell Pink Dot", "Transparent White Shell White Dot", "Silent Brown Shell Yellow Dot",
            "Gold 30M", "Gold 60M", "Gold 80M", "Gold 100M", "Silver 80M", "Dustproof Gold 80M", "Silent 30M"
        )
    )

    val scrollWheelType = FilterField(
        key = "scroll_wheel_type",
        label = "Scroll Wheel Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
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
        type = FilterType.ChipGroup(isMultiSelect = false),
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
            "PTFE Skates / Glass Skates Included"
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

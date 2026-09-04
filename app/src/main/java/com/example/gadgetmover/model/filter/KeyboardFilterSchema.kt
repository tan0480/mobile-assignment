package com.example.gadgetmover.model.filter

/**
 * The Keyboards advanced filter schema — the first category-specific schema in the redesigned
 * filter system. Every field here maps 1:1 to one of the Compose widgets in [FilterType]; the
 * common Price/Condition fields live in [CommonFilterFields] and are rendered alongside this
 * schema's [sections] by the filter sheet, not duplicated here.
 */
object KeyboardFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Ducky", "Keychron", "Logitech", "Logitech G", "Razer", "Corsair", "SteelSeries", "HyperX",
            "ASUS ROG", "ROCCAT", "Cooler Master", "NZXT", "Glorious", "Varmilo", "Leopold", "Realforce (Topre)",
            "NuPhy", "Akko", "Royal Kludge (RK)", "Epomaker", "Wooting", "Drop", "Das Keyboard", "Filco",
            "Durgod", "Obins (Anne Pro)", "Womier", "Monsgeek", "Aula", "Redragon", "Mountain", "Endgame Gear",
            "Fnatic", "Rapoo", "Dareu", "IQUNIX", "Meletrix", "KBDfans", "Other"
        )
    )

    /** A. Layout & Form Factor */
    val layoutFormFactor = FilterField(
        key = "layout_form_factor",
        label = "Layout & Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "100% Full Size (104 / 108 Keys)",
            "96% / 98% (96 - 100 Keys)",
            "80% TKL / Tenkeyless (87 / 88 Keys)",
            "75% (81 - 84 Keys)",
            "65% (67 - 68 Keys)",
            "60% (61 - 64 Keys)",
            "40% (40 - 50 Keys)",
            "Alice / Ergonomic (Split Layout)",
            "Numpad / Pad (17 - 21 Keys)",
            "Other"
        )
    )

    /** B. Switch Architecture */
    val switchType = FilterField(
        key = "switch_type",
        label = "Switch Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Mechanical",
            "Magnetic / Hall Effect (Rapid Trigger)",
            "Capacitive (Electrostatic)",
            "Optical",
            "Membrane / Scissor",
            "Other"
        )
    )

    val switchFeel = FilterField(
        key = "switch_feel",
        label = "Switch Feel",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Linear", "Tactile", "Clicky", "Silent Linear", "Silent Tactile", "Other")
    )

    val switchModel = FilterField(
        key = "switch_model",
        label = "Switch Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            // HMX
            "HMX Ziwei", "HMX Sunset Gleam", "HMX Cheese", "HMX Sillyworks Hyacinth V2/V2U",
            "HMX Canglan", "HMX Macchiato", "HMX Xinhai", "HMX Deep Navy", "HMX Eva",
            // Gateron / KTT / Outemu
            "KTT Kang White V3", "KTT Strawberry", "Gateron Oil King", "Gateron Baby Kangaroo",
            "Gateron CJ", "Gateron North Pole", "Gateron Smoothie", "Gateron Ink Black V2",
            "Gateron Magnetic Jade", "Gateron KS-20 Magnetic White",
            // TTC
            "TTC Gold Pink", "TTC Honey", "TTC Speed Silver V2", "TTC Frozen Silent V2",
            "TTC Neptune / Venus", "TTC Iron",
            // JWK / Durock
            "JWK T1", "JWK Black V2", "Durock POM Piano", "Durock Shrimp",
            // BSUN / Vertex / SW
            "Vertex V1", "BSUN BCP", "BSUN Aniya", "BSUN Raw", "BSUN Roselle", "SW Knight",
            // Leobog / Akko
            "Leobog Graywood V3/V4", "Leobog Reaper", "Leobog Building Block",
            "Akko CS Piano", "Akko V3 Cream Yellow Pro", "Akko V3 Cream Blue Pro",
            "Outemu Silent Peach V3", "Outemu Silent Lemon V3",
            // Cherry
            "Cherry MX Red", "Cherry MX Brown", "Cherry MX Blue", "Cherry MX Black",
            "Cherry MX Speed Silver", "Cherry MX Ergo Clear", "Cherry MX2A Red", "Cherry MX2A Brown",
            "Other"
        )
    )

    /** C. Keycap Architecture */
    val keycapMaterial = FilterField(
        key = "keycap_material",
        label = "Keycap Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("PBT", "ABS", "POM", "PC", "Other")
    )

    val keycapProfile = FilterField(
        key = "keycap_profile",
        label = "Keycap Profile",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "OEM", "Cherry", "XDA", "DSA", "SA", "KAT / KAM", "MDA / KDA", "MOA", "Low Profile", "Other"
        )
    )

    val keycapPrinting = FilterField(
        key = "keycap_printing",
        label = "Keycap Printing / Legend",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Double-shot", "Dye-sub", "Laser", "Side-engraved / Phantom", "Blank", "Other")
    )

    /** D. PCB Cut Specification */
    val pcbFlexCut = FilterField(
        key = "pcb_flex_cut",
        label = "PCB Flex Cut",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("No Flex Cut", "Per-key Flex Cut", "Multi-key Flex Cut", "Long / Full Flex Cut", "Other")
    )

    /** E. Power & Connectivity */
    val batteryCapacity = FilterField(
        key = "battery_capacity",
        label = "Battery Capacity",
        type = FilterType.NumberRange(min = 0f, max = 20000f, step = 500f, unit = "mAh", unitIsPrefix = false)
    )

    /** Connectivity + Polling Rate — shared architecture, see [ConnectivityPollingFields]. */

    /** F. Hardware Features & Enclosure */
    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Hot-Swappable (3-Pin / 5-Pin)",
            "Aluminum Case (CNC / Anodized)",
            "Knob Control",
            "Display Screen (TFT / OLED)",
            "QMK / VIA / Web Driver Support",
            "RGB Backlight",
            "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            layoutFormFactor,
            switchType, switchFeel, switchModel,
            keycapMaterial, keycapProfile, keycapPrinting,
            pcbFlexCut,
            batteryCapacity
        ) + ConnectivityPollingFields.fields + listOf(
            features
        )
    )
}

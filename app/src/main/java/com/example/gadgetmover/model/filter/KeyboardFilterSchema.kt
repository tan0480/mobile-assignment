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
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Ducky", "Keychron", "Logitech", "Logitech G", "Razer", "Corsair", "SteelSeries", "HyperX",
            "ASUS ROG", "ROCCAT", "Cooler Master", "NZXT", "Glorious", "Varmilo", "Leopold", "Realforce (Topre)",
            "NuPhy", "Akko", "Royal Kludge (RK)", "Epomaker", "Wooting", "Drop", "Das Keyboard", "Filco",
            "Durgod", "Obins (Anne Pro)", "Womier", "Monsgeek", "Aula", "Redragon", "Mountain", "Endgame Gear",
            "Fnatic", "Rapoo", "Dareu", "IQUNIX", "Meletrix", "KBDfans",
            "8BitDo", "Adesso", "Ajazz", "Alienware", "Angry Miao", "Apple", "Arteck", "Attack Shark",
            "Cherry / Cherry Xtrfy", "Chilkey", "Dell", "Fantech", "Fnatic Gear",
            "G.Skill", "Gigabyte / AORUS", "Havit", "HHKB", "HP / OMEN", "iKBC", "Kensington", "Kinesis",
            "Lemokey", "Lenovo / Legion", "Lofree", "LUMINKEY", "Machenike", "Mad Catz", "Matias", "MCHOSE",
            "MelGeek", "Microsoft", "Mistel", "Mode Designs", "NIZ", "Neo", "NovelKeys", "Perixx", "ProtoArc",
            "Pulsar", "Qwertykeys", "Satechi", "Tecware", "Thermaltake / Tt eSPORTS", "Trust",
            "Turtle Beach", "VGN", "Vortex", "WLMOUSE", "WOBKEY", "Work Louder", "XPG", "YUNZII", "ZSA",
            "CannonKeys", "Click Clack", "DOIO", "Evoworks", "Geonworks", "Hexgears", "KBParadise",
            "Keebwerk", "Keycool", "KPrepublic", "Matrix Lab", "MechWild", "MK", "Moon Keyboards",
            "Nuphy Field", "Odin Gaming", "Owlab", "Percent Studio", "Smith + Rune", "Shortcut Studio",
            "Shurikey", "Swagkeys", "TEX", "Ticktype", "Typone", "VGNLab", "Unknown"
        )
    )

    /** A. Layout & Form Factor */
    val layoutFormFactor = FilterField(
        key = "layout_form_factor",
        label = "Layout & Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = false),
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
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Linear", "Tactile", "Clicky", "Silent Linear", "Silent Tactile", "Other")
    )

    /**
     * Brand-then-model switch picker, e.g. picking "Gateron" narrows the model list to just
     * Gateron's own switches (see [SwitchCatalog]) — repeatable via "+ Add Switch" so a keyboard
     * built with more than one switch type can list each one. No "Other" entry in the catalogue:
     * [FilterType.SearchablePopupSelect.allowCustomInput] already lets a seller/buyer type
     * anything not listed, both here and for [brand] above.
     */
    val switchSystem = FilterField(
        key = "switch_system",
        label = "Switch Brand & Model",
        type = FilterType.SwitchSystemBuilder
    )

    /** C. Keycap Architecture */
    val keycapMaterial = FilterField(
        key = "keycap_material",
        label = "Keycap Material",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("PBT", "ABS", "POM", "PC", "Other")
    )

    val keycapProfile = FilterField(
        key = "keycap_profile",
        label = "Keycap Profile",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "OEM", "Cherry", "XDA", "DSA", "SA", "KAT / KAM", "MDA / KDA", "MOA", "Low Profile", "Other"
        )
    )

    val keycapPrinting = FilterField(
        key = "keycap_printing",
        label = "Keycap Printing / Legend",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Double-shot", "Dye-sub", "Laser", "Side-engraved / Phantom", "Blank", "Other")
    )

    /** D. PCB Cut Specification */
    val pcbFlexCut = FilterField(
        key = "pcb_flex_cut",
        label = "PCB Flex Cut",
        type = FilterType.ChipGroup(isMultiSelect = false),
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
            "RGB Backlight"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            layoutFormFactor,
            switchType, switchFeel, switchSystem,
            keycapMaterial, keycapProfile, keycapPrinting,
            pcbFlexCut,
            batteryCapacity
        ) + ConnectivityPollingFields.fields + listOf(
            features
        )
    )
}

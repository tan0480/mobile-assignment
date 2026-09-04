package com.example.gadgetmover.model.filter

/** The Memory (desktop RAM) advanced filter schema. */
object RamFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "ADATA", "Apacer", "Asgard", "ATP", "Biwin", "Buffalo", "Corsair", "Crucial", "Dell",
            "Dolphin", "Edge Memory", "Essencore Klevv", "G.Skill", "GeIL", "Gigabyte Aorus", "Goodram",
            "HP", "Hynix", "Innodisk", "Integral", "Kingbank", "Kingmax", "Kingston", "Lexar", "Micron",
            "Mushkin", "Neo Forza", "Netac", "NEMIX RAM", "OCPC", "OLOy", "Patriot", "PNY", "Qimonda",
            "Ramaxel", "Samsung", "Silicon Power", "SK hynix", "TeamGroup", "Thermaltake", "Timetec",
            "Transcend", "V-Color", "Wilk Elektronik", "XPG", "Unknown"
        )
    )

    val memoryType = FilterField(
        key = "memory_type",
        label = "Memory Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("DDR5", "DDR4", "DDR3", "Other")
    )

    val moduleFormFactor = FilterField(
        key = "module_form_factor",
        label = "Module Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("DIMM", "SO-DIMM", "ECC UDIMM", "RDIMM", "CAMM2 / LPCAMM2", "Other")
    )

    val capacityPerKit = FilterField(
        key = "capacity_per_kit",
        label = "Capacity per Kit",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("2GB", "4GB", "8GB", "16GB", "24GB", "32GB", "64GB", "96GB", "128GB", "Other")
    )

    val kitConfiguration = FilterField(
        key = "kit_configuration",
        label = "Kit Configuration",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("1 × Single Stick", "2 × Dual Channel Kit", "4 × Quad Channel Kit", "Other")
    )

    val speed = FilterField(
        key = "speed",
        label = "Speed / Frequency",
        type = FilterType.NumberRange(min = 1333f, max = 9000f, step = 200f, unit = " MHz", unitIsPrefix = false)
    )

    val casLatency = FilterField(
        key = "cas_latency",
        label = "CAS Latency (CL)",
        type = FilterType.NumberRange(min = 8f, max = 60f, step = 1f, unit = "CL", unitIsPrefix = true)
    )

    val voltage = FilterField(
        key = "voltage",
        label = "Voltage",
        type = FilterType.NumberRange(min = 1.1f, max = 1.6f, step = 0.05f, unit = "V", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options("RGB Lighting", "Low-Profile Heat Spreader", "Tall Heatsink", "XMP / EXPO Support", "ECC Support")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, memoryType, moduleFormFactor, capacityPerKit, kitConfiguration,
            speed, casLatency, voltage,
            features
        )
    )
}

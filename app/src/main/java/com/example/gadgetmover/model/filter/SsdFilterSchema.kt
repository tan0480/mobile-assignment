package com.example.gadgetmover.model.filter

/** The Solid State Drives (SSD) advanced filter schema — split out from the old combined "Storage" category. */
object SsdFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Samsung", "Western Digital", "Crucial", "Kingston", "SK hynix", "Solidigm", "KIOXIA",
            "SanDisk", "Seagate", "Lexar", "ADATA", "XPG", "TeamGroup", "Corsair", "PNY", "Sabrent",
            "Silicon Power", "Patriot", "Transcend", "Micron", "Unknown"
        )
    )

    val capacity = FilterField(
        key = "capacity",
        label = "Capacity",
        type = FilterType.NumberRangeWithUnitToggle(
            units = listOf(
                FilterType.UnitRange(unit = "GB", min = 64f, max = 2048f, step = 64f, toBaseMultiplier = 1f),
                FilterType.UnitRange(unit = "TB", min = 1f, max = 16f, step = 1f, toBaseMultiplier = 1000f)
            )
        )
    )

    private const val INTERNAL_ID = "internal"
    private const val EXTERNAL_ID = "external"
    private val externalDependency = FieldDependency("installation_type", setOf(EXTERNAL_ID))

    val installationType = FilterField(
        key = "installation_type",
        label = "Installation Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Internal", "External")
    )

    private val internalFormFactorOptions = options(
        "2.5-inch", "M.2 2230", "M.2 2242", "M.2 2260", "M.2 2280", "M.2 22110",
        "mSATA", "U.2", "U.3", "PCIe Add-in Card"
    )
    private val externalFormFactorOptions = options("Portable", "Desktop", "External Enclosure", "Expansion Card")

    /** Narrows to just the picked [installationType]'s own form-factor list — same dependent-options mechanism as [PhoneFilterSchema.socModel]. */
    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = internalFormFactorOptions + externalFormFactorOptions,
        optionsForState = { state ->
            val selected = selectedIdsFor(state, "installation_type")
            when {
                INTERNAL_ID in selected -> internalFormFactorOptions
                EXTERNAL_ID in selected -> externalFormFactorOptions
                else -> internalFormFactorOptions + externalFormFactorOptions
            }
        }
    )

    /** Only meaningful for an external drive — an internal SSD's "connector" is just its form factor above. */
    val connector = FilterField(
        key = "connector",
        label = "Connector",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("USB Type-A", "USB Type-C", "Thunderbolt"),
        visibleWhen = externalDependency
    )

    private const val SATA_ID = "sata"
    private const val NVME_ID = "nvme"

    val protocol = FilterField(
        key = "protocol",
        label = "Protocol",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("SATA", "NVMe")
    )

    val pcieGeneration = FilterField(
        key = "pcie_generation",
        label = "PCIe Generation",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("PCIe 3.0", "PCIe 4.0", "PCIe 5.0", "Not Specified"),
        visibleWhen = FieldDependency("protocol", setOf(NVME_ID))
    )

    /** For consumer SSDs, PCIe ×4 is the most relevant option. */
    val pcieLaneWidth = FilterField(
        key = "pcie_lane_width",
        label = "PCIe Lane Width",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("PCIe ×2", "PCIe ×4", "PCIe ×8"),
        visibleWhen = FieldDependency("protocol", setOf(NVME_ID))
    )

    /** SATA I is omitted — no longer relevant to normal SSD shopping. */
    val sataVersion = FilterField(
        key = "sata_version",
        label = "SATA Version",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("SATA II — 3Gb/s", "SATA III — 6Gb/s", "Not Specified"),
        visibleWhen = FieldDependency("protocol", setOf(SATA_ID))
    )

    /** Kept separate from [connector] — USB-C describes the connector shape, not its speed. */
    val externalInterfaceSpeed = FilterField(
        key = "external_interface_speed",
        label = "External Interface Speed",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "USB 3.2 Gen 1 — 5Gbps", "USB 3.2 Gen 2 — 10Gbps", "USB 3.2 Gen 2×2 — 20Gbps",
            "USB4 — 40Gbps", "Thunderbolt 3 — 40Gbps", "Thunderbolt 4 — 40Gbps", "Not Specified"
        ),
        visibleWhen = externalDependency
    )

    val readSpeed = FilterField(
        key = "read_speed",
        label = "Read Speed",
        type = FilterType.NumberRange(min = 500f, max = 14000f, step = 100f, unit = " MB/s", unitIsPrefix = false)
    )

    val writeSpeed = FilterField(
        key = "write_speed",
        label = "Write Speed",
        type = FilterType.NumberRange(min = 500f, max = 14000f, step = 100f, unit = " MB/s", unitIsPrefix = false)
    )

    val randomReadPerformance = FilterField(
        key = "random_read_performance",
        label = "Random Read Performance",
        type = FilterType.NumberRange(min = 10000f, max = 1500000f, step = 10000f, unit = " IOPS", unitIsPrefix = false)
    )

    val randomWritePerformance = FilterField(
        key = "random_write_performance",
        label = "Random Write Performance",
        type = FilterType.NumberRange(min = 10000f, max = 1500000f, step = 10000f, unit = " IOPS", unitIsPrefix = false)
    )

    val nandType = FilterField(
        key = "nand_type",
        label = "NAND Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("MLC", "TLC", "QLC", "Not Specified")
    )

    val endurance = FilterField(
        key = "endurance",
        label = "Endurance (TBW)",
        type = FilterType.NumberRange(min = 50f, max = 5000f, step = 50f, unit = " TBW", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options("DRAM Cache", "Host Memory Buffer", "Heatsink Included", "Hardware Encryption", "PS5 Compatible")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, capacity,
            installationType, formFactor, connector,
            protocol, pcieGeneration, pcieLaneWidth, sataVersion, externalInterfaceSpeed,
            readSpeed, writeSpeed, randomReadPerformance, randomWritePerformance,
            nandType, endurance,
            features
        )
    )
}

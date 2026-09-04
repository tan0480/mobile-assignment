package com.example.gadgetmover.model.filter

/** The Storage (desktop SSD/HDD) advanced filter schema. */
object StorageFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    private const val NVME_SSD_ID = "nvme_ssd"
    private const val SATA_SSD_ID = "sata_ssd"
    private const val HDD_ID = "hdd"
    private val ssdDependency = FieldDependency("storage_type", setOf(NVME_SSD_ID, SATA_SSD_ID))
    private val hddDependency = FieldDependency("storage_type", setOf(HDD_ID))

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Samsung", "Western Digital (WD)", "Seagate", "Crucial", "Kingston", "SK Hynix",
            "ADATA XPG", "Sabrent", "Corsair", "TeamGroup", "SanDisk", "Toshiba", "Other"
        )
    )

    val storageType = FilterField(
        key = "storage_type",
        label = "Storage Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("NVMe SSD", "SATA SSD", "HDD", "Other")
    )

    val capacity = FilterField(
        key = "capacity",
        label = "Capacity",
        type = FilterType.NumberRange(min = 0f, max = 24f, step = 1f, unit = "TB", unitIsPrefix = false)
    )

    val interfaceType = FilterField(
        key = "interface_type",
        label = "Interface",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("PCIe 5.0 NVMe", "PCIe 4.0 NVMe", "PCIe 3.0 NVMe", "SATA III (6Gb/s)", "Other")
    )

    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("M.2 22110", "M.2 2280", "M.2 2242", "M.2 2230", "2.5-inch", "3.5-inch", "Other")
    )

    val sequentialReadSpeed = FilterField(
        key = "sequential_read_speed",
        label = "Sequential Read Speed",
        type = FilterType.NumberRange(min = 500f, max = 14000f, step = 500f, unit = " MB/s", unitIsPrefix = false),
        visibleWhen = ssdDependency
    )

    val sequentialWriteSpeed = FilterField(
        key = "sequential_write_speed",
        label = "Sequential Write Speed",
        type = FilterType.NumberRange(min = 500f, max = 14000f, step = 500f, unit = " MB/s", unitIsPrefix = false),
        visibleWhen = ssdDependency
    )

    val nandType = FilterField(
        key = "nand_type",
        label = "NAND Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("QLC", "TLC", "MLC", "SLC Cache", "Other"),
        visibleWhen = ssdDependency
    )

    val rotationalSpeed = FilterField(
        key = "rotational_speed",
        label = "Rotational Speed",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("5400 RPM", "7200 RPM", "10000 RPM", "Other"),
        visibleWhen = hddDependency
    )

    val cacheSize = FilterField(
        key = "cache_size",
        label = "Cache Size",
        type = FilterType.NumberRange(min = 8f, max = 512f, step = 8f, unit = "MB", unitIsPrefix = false),
        visibleWhen = hddDependency
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options("Heatsink Included", "DRAM Cache", "Hardware Encryption (256-bit AES)", "NAS-Rated", "Other")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, storageType, capacity, interfaceType, formFactor,
            sequentialReadSpeed, sequentialWriteSpeed, nandType,
            rotationalSpeed, cacheSize,
            features
        )
    )
}

package com.example.gadgetmover.model.filter

/** The PC Cases advanced filter schema. */
object PcCaseFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Lian Li", "NZXT", "Corsair", "Fractal Design", "Cooler Master", "Phanteks", "HYTE",
            "be quiet!", "Thermaltake", "Antec", "DeepCool", "Montech", "SSUPD", "Jonsbo",
            "ASUS (ROG / TUF)", "MSI", "Gigabyte", "InWin", "SAMA", "Unknown"
        )
    )

    private const val PANORAMIC_GLASS_ID = "panoramic_glass_full_view"

    val caseType = FilterField(
        key = "case_type",
        label = "Case Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Full Tower", "Mid Tower", "Mini Tower", "Small Form Factor (SFF)",
            "Desktop / Horizontal", "Panoramic Glass (Full-View)", "Other"
        )
    )

    /** Kept identical to [MotherboardFilterSchema.formFactor]'s option catalogue, per an explicit request that the two stay in sync — sourced from there directly instead of a separately hand-typed copy. */
    val motherboardSupport = FilterField(
        key = "motherboard_support",
        label = "Motherboard Support",
        type = FilterType.CheckboxList,
        options = MotherboardFilterSchema.formFactor.options
    )

    /** Hidden once [caseType]'s panoramic-glass option is picked — that design doesn't have a separate solid/mesh/glass side panel to choose. */
    val sidePanelMaterial = FilterField(
        key = "side_panel_material",
        label = "Side Panel Material",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Tempered Glass", "Mesh", "Acrylic", "Solid Steel", "Other"),
        hiddenWhen = FieldDependency("case_type", setOf(PANORAMIC_GLASS_ID))
    )

    val maxGpuLength = FilterField(
        key = "max_gpu_length",
        label = "Max GPU Length",
        type = FilterType.NumberRange(min = 250f, max = 500f, step = 10f, unit = "mm", unitIsPrefix = false)
    )

    val maxCpuCoolerHeight = FilterField(
        key = "max_cpu_cooler_height",
        label = "Max CPU Cooler Height",
        type = FilterType.NumberRange(min = 130f, max = 200f, step = 5f, unit = "mm", unitIsPrefix = false)
    )

    /** Installable fan capacity (how many mounts the case has), not how many fans ship pre-installed — split by size since a case's 120mm and 140mm mount counts differ. */
    val fanMounts120mm = FilterField(
        key = "fan_mounts_120mm",
        label = "120mm Fan Mounts",
        type = FilterType.NumberRange(min = 1f, max = 10f, step = 1f, unit = " Fans", unitIsPrefix = false)
    )

    val fanMounts140mm = FilterField(
        key = "fan_mounts_140mm",
        label = "140mm Fan Mounts",
        type = FilterType.NumberRange(min = 1f, max = 10f, step = 1f, unit = " Fans", unitIsPrefix = false)
    )

    val radiatorSupport = FilterField(
        key = "radiator_support",
        label = "Radiator Support",
        type = FilterType.CheckboxList,
        options = options("120mm", "240mm", "280mm", "360mm", "420mm", "Other")
    )

    val hddBays35 = FilterField(
        key = "hdd_bays_3_5",
        label = "3.5-inch HDD Bays",
        type = FilterType.NumberRange(min = 0f, max = 10f, step = 1f, unit = " Bays", unitIsPrefix = false)
    )

    val frontIo = FilterField(
        key = "front_io",
        label = "Front I/O",
        type = FilterType.CheckboxList,
        options = options("USB-C", "USB 3.0", "USB 2.0", "3.5mm Audio Jack", "Other")
    )

    /** Each USB [frontIo] type reveals its own port-count range once selected — same pattern as [MotherboardFilterSchema]'s Rear USB Ports. */
    private fun frontIoPortCountField(key: String, label: String, optionId: String) = FilterField(
        key = key,
        label = "$label Port Count",
        type = FilterType.NumberRange(min = 0f, max = 4f, step = 1f, unit = " Ports", unitIsPrefix = false),
        visibleWhen = FieldDependency("front_io", setOf(optionId))
    )

    val usbCPortCount = frontIoPortCountField("front_io_usb_c_port_count", "USB-C", "usb_c")
    val usb30PortCount = frontIoPortCountField("front_io_usb_3_0_port_count", "USB 3.0", "usb_3_0")
    val usb20PortCount = frontIoPortCountField("front_io_usb_2_0_port_count", "USB 2.0", "usb_2_0")

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options("LCD Display", "Back-Connect Motherboard Support")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, caseType, motherboardSupport, sidePanelMaterial,
            maxGpuLength, maxCpuCoolerHeight,
            fanMounts120mm, fanMounts140mm, radiatorSupport,
            hddBays35,
            frontIo, usbCPortCount, usb30PortCount, usb20PortCount,
            features
        )
    )
}

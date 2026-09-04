package com.example.gadgetmover.model.filter

/** The Graphics Cards (dGPU) advanced filter schema. */
object GraphicsCardFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Acer", "AMD", "AFOX", "ASRock", "ASUS", "Biostar", "Colorful", "Dell", "Diamond Multimedia",
            "Elsa", "EVGA", "Foxconn", "Gainward", "GALAX", "Gigabyte", "HIS", "HP", "Inno3D", "Intel",
            "KFA2", "Leadtek", "Lenovo", "Manli", "Matrox", "Maxsun", "MSI", "NVIDIA", "Palit", "PNY",
            "PowerColor", "Sapphire", "Sparkle", "VisionTek", "XFX", "Yeston", "Zotac", "Unknown"
        )
    )

    val chipsetBrand = FilterField(
        key = "chipset_brand",
        label = "Chipset Brand",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("NVIDIA GeForce", "AMD Radeon", "Intel Arc", "Other")
    )

    private val gpuModelsByChipsetBrand: Map<String, List<FilterOption>> = mapOf(
        "nvidia_geforce" to options(
            "NVIDIA GeForce RTX 5090", "NVIDIA GeForce RTX 5080", "NVIDIA GeForce RTX 5070 Ti",
            "NVIDIA GeForce RTX 5070", "NVIDIA GeForce RTX 5060 Ti", "NVIDIA GeForce RTX 5060",
            "NVIDIA GeForce RTX 4090", "NVIDIA GeForce RTX 4080 Super", "NVIDIA GeForce RTX 4080",
            "NVIDIA GeForce RTX 4070 Ti Super", "NVIDIA GeForce RTX 4070 Ti", "NVIDIA GeForce RTX 4070 Super",
            "NVIDIA GeForce RTX 4070", "NVIDIA GeForce RTX 4060 Ti", "NVIDIA GeForce RTX 4060",
            "NVIDIA GeForce RTX 3090 Ti", "NVIDIA GeForce RTX 3090", "NVIDIA GeForce RTX 3080 Ti",
            "NVIDIA GeForce RTX 3080", "NVIDIA GeForce RTX 3070 Ti", "NVIDIA GeForce RTX 3070",
            "NVIDIA GeForce RTX 3060 Ti", "NVIDIA GeForce RTX 3060", "NVIDIA GeForce RTX 3050",
            "NVIDIA GeForce GTX 1660 Super", "NVIDIA GeForce GTX 1660 Ti", "NVIDIA GeForce GTX 1650"
        ),
        "amd_radeon" to options(
            "AMD Radeon RX 9070 XT", "AMD Radeon RX 9070", "AMD Radeon RX 7900 XTX", "AMD Radeon RX 7900 XT",
            "AMD Radeon RX 7800 XT", "AMD Radeon RX 7700 XT", "AMD Radeon RX 7600 XT", "AMD Radeon RX 7600",
            "AMD Radeon RX 6950 XT", "AMD Radeon RX 6900 XT", "AMD Radeon RX 6800 XT", "AMD Radeon RX 6800",
            "AMD Radeon RX 6700 XT", "AMD Radeon RX 6600 XT", "AMD Radeon RX 6600"
        ),
        "intel_arc" to options("Intel Arc B580", "Intel Arc B570", "Intel Arc A770", "Intel Arc A750", "Intel Arc A380"),
        "other" to emptyList()
    )

    /** Narrows to just the picked [chipsetBrand]'s chips — same dependent-options mechanism as [PhoneFilterSchema.socModel]. */
    val gpuModel = FilterField(
        key = "gpu_model",
        label = "GPU Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = gpuModelsByChipsetBrand.values.flatten().distinctBy { it.id },
        optionsForState = { state ->
            val selectedBrandIds = selectedIdsFor(state, "chipset_brand")
            val matched = selectedBrandIds.flatMap { gpuModelsByChipsetBrand[it] ?: emptyList() }.distinctBy { it.id }
            matched.ifEmpty { gpuModelsByChipsetBrand.values.flatten().distinctBy { it.id } }
        }
    )

    val vramCapacity = FilterField(
        key = "vram_capacity",
        label = "VRAM Capacity",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("4GB", "6GB", "8GB", "10GB", "12GB", "16GB", "20GB", "24GB", "32GB", "Other")
    )

    val vramType = FilterField(
        key = "vram_type",
        label = "VRAM Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("GDDR7", "GDDR6X", "GDDR6", "GDDR5", "HBM2", "Other")
    )

    val memoryBusWidth = FilterField(
        key = "memory_bus_width",
        label = "Memory Bus Width",
        type = FilterType.NumberRange(min = 64f, max = 512f, step = 32f, unit = "-bit", unitIsPrefix = false)
    )

    val boostClock = FilterField(
        key = "boost_clock",
        label = "Boost Clock",
        type = FilterType.NumberRange(min = 1500f, max = 3200f, step = 50f, unit = " MHz", unitIsPrefix = false)
    )

    val tdp = FilterField(
        key = "tdp",
        label = "TDP (Power Consumption)",
        type = FilterType.NumberRange(min = 50f, max = 600f, step = 10f, unit = "W", unitIsPrefix = false)
    )

    val powerConnector = FilterField(
        key = "power_connector",
        label = "Power Connector",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "12VHPWR / 12V-2x6", "8-Pin × 1", "8-Pin × 2", "8-Pin × 3",
            "6-Pin", "No External Power Required", "Other"
        )
    )

    val coolingDesign = FilterField(
        key = "cooling_design",
        label = "Cooling Design",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "Blower", "Single Fan", "Dual Fan", "Triple Fan", "Quad Fan",
            "Water-Cooled (AIO)", "Hybrid (Air + Water)", "Other"
        )
    )

    val cardLength = FilterField(
        key = "card_length",
        label = "Card Length",
        type = FilterType.NumberRange(min = 150f, max = 400f, step = 10f, unit = "mm", unitIsPrefix = false)
    )

    val slotWidth = FilterField(
        key = "slot_width",
        label = "Slot Width",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("2-Slot", "2.5-Slot", "3-Slot", "3.5-Slot", "4-Slot", "Other")
    )

    val outputPorts = FilterField(
        key = "output_ports",
        label = "Output Ports",
        type = FilterType.CheckboxList,
        options = options("HDMI 2.1", "DisplayPort 2.1", "DisplayPort 1.4a", "USB-C", "Other")
    )

    /** Each [outputPorts] type reveals its own port-count range once selected — same pattern as [MotherboardFilterSchema]'s Rear USB Ports. */
    private fun outputPortCountField(key: String, label: String, optionId: String) = FilterField(
        key = key,
        label = "$label Port Count",
        type = FilterType.NumberRange(min = 0f, max = 4f, step = 1f, unit = " Ports", unitIsPrefix = false),
        visibleWhen = FieldDependency("output_ports", setOf(optionId))
    )

    val hdmi21PortCount = outputPortCountField("hdmi_2_1_port_count", "HDMI 2.1", "hdmi_2_1")
    val displayPort21PortCount = outputPortCountField("displayport_2_1_port_count", "DisplayPort 2.1", "displayport_2_1")
    val displayPort14aPortCount = outputPortCountField("displayport_1_4a_port_count", "DisplayPort 1.4a", "displayport_1_4a")
    val usbCPortCount = outputPortCountField("usb_c_port_count", "USB-C", "usb_c")

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Ray Tracing Support", "DLSS 4 / DLSS 3", "AMD FSR 4 / FSR 3", "Intel XeSS",
            "RGB Lighting", "ARGB Header", "Backplate Included", "Factory Overclocked"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, chipsetBrand, gpuModel,
            vramCapacity, vramType, memoryBusWidth,
            boostClock, tdp, powerConnector, coolingDesign,
            cardLength, slotWidth, outputPorts,
            hdmi21PortCount, displayPort21PortCount, displayPort14aPortCount, usbCPortCount,
            features
        )
    )
}

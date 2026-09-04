package com.example.gadgetmover.model.filter

/** The Motherboards advanced filter schema. Socket options are shared with [CpuFilterSchema] and [CpuCoolerFilterSchema] via [PcSocketOptions]. */
object MotherboardFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Acer", "AFOX", "Albatron", "Alienware", "ASRock", "ASUS", "Biostar", "Colorful", "Dell",
            "DFI", "ECS", "EVGA", "Foxconn", "Fujitsu", "Gigabyte", "HP", "Intel", "Jetway", "Kontron",
            "Lenovo", "Machinist", "Maxsun", "MiTAC", "MSI", "NZXT", "Onda", "Pegatron", "Sapphire",
            "Shuttle", "SOYO", "Supermicro", "Tyan", "Zotac", "Unknown"
        )
    )

    val socket = FilterField(
        key = "socket",
        label = "Socket",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = PcSocketOptions.options.filter { it.id != "other" }
    )

    /**
     * Narrows [chipset] to just the picked [socket]'s chips. Workstation-tier pairings (the
     * Xeon-facing C2xx/C6xx/C741/W-series chipsets) are grouped with whichever consumer socket
     * generation they physically share, per Intel's published platform pairings — except W790,
     * which uniquely takes LGA 4677 rather than its contemporaries' LGA 1700. C261/C262/C266 and
     * W880 have thinner public documentation than the rest of this table; they're placed by
     * generation contemporaries (LGA 1700 and LGA 1851 respectively) as a best-effort default —
     * flag it if either turns out to be wrong.
     */
    private val chipsetsBySocket: Map<String, List<FilterOption>> = mapOf(
        "lga_1155" to options("H61", "B65", "Q65", "H67", "P67", "Q67", "Z68", "B75", "H77", "Q75", "Q77", "Z75", "Z77", "C202", "C204", "C206", "C216"),
        "lga_1150" to options("H81", "B85", "Q85", "Q87", "H87", "Z87", "H97", "Z97", "C222", "C224", "C226"),
        "lga_1151" to options(
            "H110", "B150", "Q150", "H170", "Q170", "Z170", "B250", "Q250", "H270", "Q270", "Z270",
            "H310", "B360", "B365", "H370", "Q370", "Z370", "Z390", "C232", "C236", "C242", "C246"
        ),
        "lga_1200" to options("H410", "B460", "H470", "Q470", "W480", "Z490", "H510", "B560", "H570", "Q570", "W580", "Z590"),
        "lga_1700" to options("H610", "B660", "H670", "Q670", "W680", "Z690", "B760", "H770", "Q770", "Z790", "C252", "C256", "C261", "C262", "C266"),
        "lga_1851" to options("H810", "B860", "Q870", "W880", "Z890"),
        "lga_3647" to options("C621", "C621A", "C627"),
        "lga_4677" to options("C741", "W790"),
        "am3_plus" to options("970", "990X", "990FX"),
        "fm1" to options("A55", "A75"),
        "fm2_plus" to options("A58", "A68H", "A78", "A88X"),
        "am4" to options("A300", "A320", "B350", "X370", "B450", "X470", "A520", "B550", "X570"),
        "am5" to options("A620", "B650", "B650E", "X670", "X670E", "B840", "B850", "X870", "X870E"),
        "tr4" to options("X399"),
        "strx4" to options("TRX40"),
        "swrx8" to options("WRX0"),
        "str5" to options("TRX50"),
        "other" to emptyList()
    )

    /** Narrows to just the picked [socket]'s chips — same dependent-options mechanism as [PhoneFilterSchema.socModel]. */
    val chipset = FilterField(
        key = "chipset",
        label = "Chipset",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = chipsetsBySocket.values.flatten().distinctBy { it.id },
        optionsForState = { state ->
            val selectedSocketIds = selectedIdsFor(state, "socket")
            val matched = selectedSocketIds.flatMap { chipsetsBySocket[it] ?: emptyList() }.distinctBy { it.id }
            matched.ifEmpty { chipsetsBySocket.values.flatten().distinctBy { it.id } }
        }
    )

    val connectorLayout = FilterField(
        key = "connector_layout",
        label = "Connector Layout",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Standard (Front Connectors)", "Back-Connect (Rear Connectors)", "Other")
    )

    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("E-ATX", "ATX", "Micro-ATX", "Mini-ITX", "Other")
    )

    val memoryType = FilterField(
        key = "memory_type",
        label = "Memory Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("DDR5", "DDR4", "DDR3", "Other")
    )

    val memorySlots = FilterField(
        key = "memory_slots",
        label = "Memory Slots",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("2 Slots", "4 Slots", "8 Slots", "Other")
    )

    val maxMemoryCapacity = FilterField(
        key = "max_memory_capacity",
        label = "Max Memory Capacity",
        type = FilterType.NumberRange(min = 32f, max = 256f, step = 16f, unit = "GB", unitIsPrefix = false)
    )

    /** Repeatable "Add PCIe Slot" builder — pick a lane width, pick a generation, Confirm, then add another. Multiple slots are ANDed together (a board must have all of them). See [PcieSlotRequirement]. */
    val pcieSlots = FilterField(
        key = "pcie_slots",
        label = "PCIe Slots",
        type = FilterType.PcieSlotBuilder
    )

    val m2Slots = FilterField(
        key = "m2_slots",
        label = "M.2 Slots",
        type = FilterType.NumberRange(min = 1f, max = 5f, step = 1f, unit = " Slots", unitIsPrefix = false)
    )

    val sataPorts = FilterField(
        key = "sata_ports",
        label = "SATA Ports",
        type = FilterType.NumberRange(min = 0f, max = 8f, step = 1f, unit = " Ports", unitIsPrefix = false)
    )

    // --- Rear I/O: Display Outputs ---

    val displayOutputs = FilterField(
        key = "display_outputs",
        label = "Display Outputs",
        type = FilterType.CheckboxList,
        options = options("HDMI", "DisplayPort", "USB-C DisplayPort", "DVI", "VGA")
    )

    // --- Rear I/O: USB Ports (each reveals a port-count range once selected) ---

    val rearIoUsbPorts = FilterField(
        key = "rear_io_usb_ports",
        label = "Rear USB Ports",
        type = FilterType.CheckboxList,
        options = options(
            "USB 2.0 Type-A", "USB 3.2 Gen 1 Type-A", "USB 3.2 Gen 2 Type-A", "USB 3.2 Gen 2x2 Type-A",
            "USB 3.2 Gen 1 Type-C", "USB 3.2 Gen 2 Type-C", "USB 3.2 Gen 2x2 Type-C", "USB4 Type-C"
        )
    )

    private fun usbPortCountField(key: String, label: String, optionId: String) = FilterField(
        key = key,
        label = "$label Port Count",
        type = FilterType.NumberRange(min = 0f, max = 8f, step = 1f, unit = " Ports", unitIsPrefix = false),
        visibleWhen = FieldDependency("rear_io_usb_ports", setOf(optionId))
    )

    val usb2TypeACount = usbPortCountField("usb_2_type_a_count", "USB 2.0 Type-A", "usb_2_0_type_a")
    val usb32Gen1TypeACount = usbPortCountField("usb_3_2_gen_1_type_a_count", "USB 3.2 Gen 1 Type-A", "usb_3_2_gen_1_type_a")
    val usb32Gen2TypeACount = usbPortCountField("usb_3_2_gen_2_type_a_count", "USB 3.2 Gen 2 Type-A", "usb_3_2_gen_2_type_a")
    val usb32Gen2x2TypeACount = usbPortCountField("usb_3_2_gen_2x2_type_a_count", "USB 3.2 Gen 2x2 Type-A", "usb_3_2_gen_2x2_type_a")
    val usb32Gen1TypeCCount = usbPortCountField("usb_3_2_gen_1_type_c_count", "USB 3.2 Gen 1 Type-C", "usb_3_2_gen_1_type_c")
    val usb32Gen2TypeCCount = usbPortCountField("usb_3_2_gen_2_type_c_count", "USB 3.2 Gen 2 Type-C", "usb_3_2_gen_2_type_c")
    val usb32Gen2x2TypeCCount = usbPortCountField("usb_3_2_gen_2x2_type_c_count", "USB 3.2 Gen 2x2 Type-C", "usb_3_2_gen_2x2_type_c")
    val usb4TypeCCount = usbPortCountField("usb4_type_c_count", "USB4 Type-C", "usb4_type_c")

    // --- Rear I/O: everything else (13 options, over the 10-option threshold, so a searchable popup instead of a chip grid) ---

    val otherRearIoPorts = FilterField(
        key = "other_rear_io_ports",
        label = "Other Rear I/O Ports",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = false),
        options = options(
            "RJ-45 Ethernet", "3.5mm Line-in", "3.5mm Line-out", "Mic-in", "Rear Speaker",
            "Optical S/PDIF", "PS/2 Keyboard", "PS/2 Mouse", "Clear CMOS", "BIOS Flashback",
            "Wi-Fi Antenna", "Coaxial", "Serial Port"
        )
    )

    val rj45PortSpeed = FilterField(
        key = "rj45_port_speed",
        label = "RJ-45 Port Speed",
        type = FilterType.NumberRange(min = 1f, max = 10f, step = 1f, unit = "Gbps", unitIsPrefix = false),
        visibleWhen = FieldDependency("other_rear_io_ports", setOf("rj_45_ethernet"))
    )

    // --- Front Panel Headers ---

    val frontPanelHeaders = FilterField(
        key = "front_panel_headers",
        label = "Front Panel Headers",
        type = FilterType.CheckboxList,
        options = options(
            "USB 2.0 Header", "USB 3.2 Gen 1 Header", "USB 3.2 Gen 2 Header",
            "USB 3.2 Gen 2x2 Type-E Header", "USB-C Front Panel Header", "HD Audio Header"
        )
    )

    val argbHeaders = FilterField(
        key = "argb_headers",
        label = "ARGB Headers (0-6+)",
        type = FilterType.NumberRange(min = 0f, max = 6f, step = 1f, unit = " Headers", unitIsPrefix = false)
    )

    val rgbHeaders = FilterField(
        key = "rgb_headers",
        label = "RGB Headers (0-4+)",
        type = FilterType.NumberRange(min = 0f, max = 4f, step = 1f, unit = " Headers", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Wi-Fi Built-in", "Bluetooth Built-in", "Dual LAN (Dual Ethernet Ports)", "RGB / ARGB Headers",
            "BIOS Flashback", "Reinforced PCIe Slot (Steel Armor)"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, socket, chipset, connectorLayout, formFactor,
            memoryType, memorySlots, maxMemoryCapacity,
            pcieSlots, m2Slots, sataPorts,
            displayOutputs,
            rearIoUsbPorts,
            usb2TypeACount, usb32Gen1TypeACount, usb32Gen2TypeACount, usb32Gen2x2TypeACount,
            usb32Gen1TypeCCount, usb32Gen2TypeCCount, usb32Gen2x2TypeCCount, usb4TypeCCount,
            otherRearIoPorts, rj45PortSpeed,
            frontPanelHeaders, argbHeaders, rgbHeaders,
            features
        )
    )
}

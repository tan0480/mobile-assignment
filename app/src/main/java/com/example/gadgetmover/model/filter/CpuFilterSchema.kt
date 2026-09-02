package com.example.gadgetmover.model.filter

/** The Processors (desktop CPU) advanced filter schema. */
object CpuFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options("Intel", "AMD", "Other")
    )

    val socket = FilterField(
        key = "socket",
        label = "Socket",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(*PcSocketOptions.labels)
    )

    private val cpuModelsByBrand: Map<String, List<FilterOption>> = mapOf(
        "intel" to options(
            "Core Ultra 9 285K", "Core Ultra 7 265K", "Core Ultra 7 265KF", "Core Ultra 5 245K", "Core Ultra 5 245KF",
            "Core i9-14900K", "Core i9-14900KS", "Core i9-14900", "Core i7-14700K", "Core i7-14700", "Core i5-14600K", "Core i5-14400", "Core i3-14100",
            "Core i9-13900K", "Core i9-13900KS", "Core i7-13700K", "Core i5-13600K", "Core i5-13400", "Core i3-13100",
            "Core i9-12900K", "Core i9-12900KS", "Core i7-12700K", "Core i5-12600K", "Core i5-12400", "Core i3-12100",
            "Core i9-11900K", "Core i7-11700K", "Core i5-11600K", "Core i5-11400", "Core i3-10100",
            "Core i9-10900K", "Core i7-10700K", "Core i5-10600K", "Core i5-10400",
            "Core i9-9900K", "Core i9-9900KS", "Core i7-9700K", "Core i5-9600K",
            "Core i7-8700K", "Core i5-8600K", "Core i3-8100",
            "Core i7-7700K", "Core i5-7600K",
            "Core i7-6700K", "Core i5-6600K",
            "Xeon W9-3495X", "Xeon W7-3465X", "Xeon W5-3425", "Xeon W-2295", "Xeon W-3175X",
            "Xeon Platinum 8592+", "Xeon Platinum 8280", "Xeon Gold 6448Y", "Xeon Gold 6248",
            "Xeon 6980P (Xeon 6)", "Xeon E-2388G"
        ),
        "amd" to options(
            "Ryzen 9 9950X3D", "Ryzen 9 9950X", "Ryzen 9 9900X", "Ryzen 7 9800X3D", "Ryzen 7 9700X", "Ryzen 5 9600X", "Ryzen 5 9600",
            "Ryzen 9 7950X3D", "Ryzen 9 7950X", "Ryzen 9 7900X", "Ryzen 7 7800X3D", "Ryzen 7 7700X", "Ryzen 5 7600X", "Ryzen 5 7600",
            "Ryzen 9 5950X", "Ryzen 9 5900X", "Ryzen 7 5800X3D", "Ryzen 7 5800X", "Ryzen 5 5600X", "Ryzen 5 5600", "Ryzen 5 5500",
            "Ryzen 9 3900X", "Ryzen 7 3700X", "Ryzen 5 3600", "Ryzen 5 3600X",
            "Ryzen 7 2700X", "Ryzen 5 2600X", "Ryzen 5 2600",
            "Ryzen 7 1800X", "Ryzen 7 1700X", "Ryzen 5 1600X", "Ryzen 5 1600",
            "Ryzen Threadripper PRO 7995WX", "Ryzen Threadripper PRO 7985WX", "Ryzen Threadripper PRO 7975WX", "Ryzen Threadripper PRO 5995WX",
            "Ryzen Threadripper 7980X", "Ryzen Threadripper 7970X", "Ryzen Threadripper 7960X", "Ryzen Threadripper 3990X", "Ryzen Threadripper 3970X", "Ryzen Threadripper 2990WX",
            "EPYC 9754", "EPYC 9654", "EPYC 7763", "EPYC 7742", "EPYC 4585PX"
        ),
        "other" to options("Other")
    )

    /** Narrows to just the picked [brand]'s chips — same dependent-options mechanism [PhoneFilterSchema.socModel] introduced (this field used to just keep one brand-tagged flat list, per an older note here; now it actually filters). Covers roughly the last 10 years of desktop models, including enterprise-grade (Xeon/EPYC/Threadripper). */
    val cpuModel = FilterField(
        key = "cpu_model",
        label = "CPU Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = cpuModelsByBrand.values.flatten().distinctBy { it.id },
        optionsForState = { state ->
            val selectedBrandIds = (state.valueFor("brand") as? FilterFieldValue.MultiSelect)?.selectedIds ?: emptySet()
            val matched = selectedBrandIds.flatMap { cpuModelsByBrand[it] ?: emptyList() }.distinctBy { it.id }
            matched.ifEmpty { cpuModelsByBrand.values.flatten().distinctBy { it.id } }
        }
    )

    val coreArchitecture = FilterField(
        key = "core_architecture",
        label = "Core Architecture",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Hybrid Architecture (Performance-cores + Efficient-cores)", "Pure Performance Big-cores (All Full-spec Cores)", "Other")
    )

    val coreCount = FilterField(
        key = "core_count",
        label = "Core Count",
        type = FilterType.NumberRange(min = 2f, max = 64f, step = 2f, unit = " Cores", unitIsPrefix = false)
    )

    val threadCount = FilterField(
        key = "thread_count",
        label = "Thread Count",
        type = FilterType.NumberRange(min = 2f, max = 128f, step = 2f, unit = " Threads", unitIsPrefix = false)
    )

    val baseClock = FilterField(
        key = "base_clock",
        label = "Base Clock",
        type = FilterType.NumberRange(min = 1.5f, max = 4.5f, step = 0.1f, unit = " GHz", unitIsPrefix = false)
    )

    val boostClock = FilterField(
        key = "boost_clock",
        label = "Boost Clock",
        type = FilterType.NumberRange(min = 3.5f, max = 6.2f, step = 0.1f, unit = " GHz", unitIsPrefix = false)
    )

    val tdp = FilterField(
        key = "tdp",
        label = "TDP",
        type = FilterType.NumberRange(min = 35f, max = 280f, step = 5f, unit = "W", unitIsPrefix = false)
    )

    val memorySupport = FilterField(
        key = "memory_support",
        label = "Memory Support",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("DDR5 Only", "DDR4 Only", "DDR5 & DDR4 Compatible")
    )

    val nativePcieRevision = FilterField(
        key = "native_pcie_revision",
        label = "Native PCIe Revision",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("PCIe 5.0", "PCIe 4.0", "PCIe 3.0")
    )

    val cpuPcieLanes = FilterField(
        key = "cpu_pcie_lanes",
        label = "CPU PCIe Lanes",
        type = FilterType.NumberRange(min = 16f, max = 128f, step = 4f, unit = " Lanes", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Integrated Graphics", "Unlocked Multiplier (Overclockable)", "Stock Cooler Included",
            "ECC Memory Support", "3D V-Cache", "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, socket, cpuModel,
            coreCount, threadCount, coreArchitecture, baseClock, boostClock, tdp,
            memorySupport, nativePcieRevision, cpuPcieLanes,
            features
        )
    )
}

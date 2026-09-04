package com.example.gadgetmover.model.filter

/**
 * The Laptops advanced filter schema — the CPU/GPU searchable-popup catalogues were extended
 * beyond what was originally specified to span roughly the last 10 years of mobile silicon
 * (2015/2016 onward through 2025), not just the newest generation, per an explicit request to
 * fill in older models. "Other" always remains the catch-all for anything still missing.
 */
object LaptopFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    private const val DUAL_SCREEN_ID = "dual_screen_laptop"
    private const val DGPU_ID = "dedicated_graphics_dgpu"

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Acer", "Aftershock", "Alienware", "Apple", "ASUS", "AVITA", "Chuwi", "Clevo", "Colorful",
            "Corsair", "CyberPowerPC", "Dell", "Dynabook", "Eluktronics", "Eurocom", "Framework", "Fujitsu",
            "Gateway", "Gigabyte", "Google", "Honor", "HP", "Huawei", "Infinix", "LG", "Lenovo", "Maingear",
            "Medion", "Microsoft", "MSI", "Monster Notebook", "NEC", "Nexstgo", "Nokia", "Origin PC",
            "Panasonic", "Razer", "Realme", "Samsung", "Schenker", "System76", "Toshiba", "Vaio", "Venom",
            "Xiaomi", "XMG", "Unknown"
        )
    )

    val laptopCategory = FilterField(
        key = "laptop_category",
        label = "Laptop Category",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "Thin & Light (Ultrabook)", "Creator / Performance All-Rounder", "Gaming Laptop",
            "Mobile Workstation", "2-in-1 Convertible (360° Flip / Touch)",
            "2-in-1 Detachable (Tablet + Keyboard)", "Dual-Screen Laptop", "Other"
        )
    )

    // --- Display & Screen ---

    val primaryScreenSize = FilterField(
        key = "primary_screen_size",
        label = "Primary Screen Size",
        type = FilterType.NumberRange(min = 11.6f, max = 18.0f, step = 0.5f, unit = "\"", unitIsPrefix = false)
    )

    val secondaryScreenSize = FilterField(
        key = "secondary_screen_size",
        label = "Secondary Screen Size",
        type = FilterType.NumberRange(min = 8.0f, max = 16.0f, step = 0.5f, unit = "\"", unitIsPrefix = false),
        visibleWhen = FieldDependency("laptop_category", setOf(DUAL_SCREEN_ID))
    )

    val aspectRatio = FilterField(
        key = "aspect_ratio",
        label = "Aspect Ratio",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("16:10", "16:9", "3:2", "Other")
    )

    val panelType = FilterField(
        key = "panel_type",
        label = "Panel Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("OLED", "Mini-LED", "Fast IPS", "IPS", "VA", "TN", "Other")
    )

    val resolution = FilterField(
        key = "resolution",
        label = "Resolution",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "1920 × 1080 (FHD)", "1920 × 1200 (WUXGA / FHD+)", "2560 × 1440 (QHD / 2K)",
            "2560 × 1600 (WQXGA / 2.5K)", "2880 × 1800 (2.8K)", "3200 × 2000 (3.2K)",
            "3840 × 2160 (UHD / 4K)", "3840 × 2400 (WQUXGA / 4K+)", "Other"
        )
    )

    val refreshRate = FilterField(
        key = "refresh_rate",
        label = "Refresh Rate",
        type = FilterType.NumberRange(min = 60f, max = 500f, step = 10f, unit = " Hz", unitIsPrefix = false)
    )

    val srgbCoverage = FilterField(
        key = "srgb_coverage",
        label = "Color Gamut — sRGB Coverage",
        type = FilterType.NumberRange(min = 90f, max = 100f, step = 1f, unit = "%", unitIsPrefix = false)
    )

    val dciP3Coverage = FilterField(
        key = "dci_p3_coverage",
        label = "Color Gamut — DCI-P3 Coverage",
        type = FilterType.NumberRange(min = 90f, max = 100f, step = 1f, unit = "%", unitIsPrefix = false)
    )

    val adobeRgbCoverage = FilterField(
        key = "adobe_rgb_coverage",
        label = "Color Gamut — Adobe RGB Coverage",
        type = FilterType.NumberRange(min = 90f, max = 100f, step = 1f, unit = "%", unitIsPrefix = false)
    )

    val peakBrightness = FilterField(
        key = "peak_brightness",
        label = "Peak Brightness",
        type = FilterType.NumberRange(min = 300f, max = 1600f, step = 50f, unit = " nits", unitIsPrefix = false)
    )

    val screenFeatures = FilterField(
        key = "screen_features",
        label = "Screen Features",
        type = FilterType.CheckboxList,
        options = options("Matte / Anti-Glare", "Glossy", "Touchscreen", "Stylus / Active Pen Support", "Other")
    )

    // --- Processor (CPU) ---

    val cpuBrand = FilterField(
        key = "cpu_brand",
        label = "CPU Brand",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Intel", "AMD", "Apple Silicon", "Qualcomm Snapdragon", "Other")
    )

    private val cpuModelsByBrand: Map<String, List<FilterOption>> = mapOf(
        "intel" to options(
            "i7-6700HQ", "i7-6820HK", "i5-6300HQ", "i7-7500U", "i7-7700HQ", "i7-7820HK",
            "i5-8250U", "i5-8300H", "i7-8550U", "i7-8750H", "i9-8950HK",
            "i5-9300H", "i7-9750H", "i9-9980HK",
            "i5-10210U", "i7-1065G7", "i7-10750H", "i9-10980HK",
            "i5-1135G7", "i7-1165G7", "i7-11800H", "i9-11980HK",
            "i5-1240P", "i5-12500H", "i7-1260P", "i7-12700H", "i9-12900HK",
            "i5-1335U", "i5-13500H", "i7-1360P", "i7-13700H", "i9-13980HX",
            "Core Ultra 5 125H", "Core Ultra 7 155H", "Core Ultra 9 185H",
            "Core Ultra 5 225H", "Core Ultra 7 255H", "Core Ultra 9 285HX"
        ),
        "amd" to options(
            "Ryzen 5 2500U", "Ryzen 7 2700U",
            "Ryzen 5 3550H", "Ryzen 7 3750H",
            "Ryzen 5 4500U", "Ryzen 7 4800H", "Ryzen 9 4900HS",
            "Ryzen 5 5500U", "Ryzen 7 5800H", "Ryzen 9 5900HX",
            "Ryzen 5 6600U", "Ryzen 7 6800H", "Ryzen 9 6900HX",
            "Ryzen 5 7530U", "Ryzen 7 7840U", "Ryzen 9 7945HX",
            "Ryzen AI 9 HX 370", "Ryzen AI 9 365", "Ryzen AI Max+ 395"
        ),
        "apple_silicon" to options(
            // Apple Silicon (2020-2025)
            "Apple M4 Max", "Apple M4 Pro", "Apple M4",
            "Apple M3 Max", "Apple M3 Pro", "Apple M3",
            "Apple M2 Max / M2 Pro / M2", "Apple M1 Max / M1 Pro / M1"
        ),
        "qualcomm_snapdragon" to options(
            // Qualcomm Snapdragon (2018-2024)
            "Snapdragon X Elite (X1E-84-100 / X1E-80-100 / X1E-78-100)",
            "Snapdragon X Plus (X1P-64-100 / X1P-42-100)",
            "Snapdragon 8cx Gen 3", "Snapdragon 8cx Gen 2", "Snapdragon 8cx (Gen 1)"
        ),
        "other" to emptyList()
    )

    /** Narrows to just the picked [cpuBrand]'s chips — same dependent-options mechanism as [PhoneFilterSchema.socModel]. */
    val cpuModel = FilterField(
        key = "cpu_model",
        label = "CPU Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = cpuModelsByBrand.values.flatten().distinctBy { it.id },
        optionsForState = { state ->
            val selectedBrandIds = selectedIdsFor(state, "cpu_brand")
            val matched = selectedBrandIds.flatMap { cpuModelsByBrand[it] ?: emptyList() }.distinctBy { it.id }
            matched.ifEmpty { cpuModelsByBrand.values.flatten().distinctBy { it.id } }
        }
    )

    val npuTops = FilterField(
        key = "npu_tops",
        label = "Integrated NPU TOPS (AI TOPS)",
        type = FilterType.NumberRange(min = 10f, max = 55f, step = 5f, unit = " TOPS", unitIsPrefix = false)
    )

    // --- Graphics (GPU) ---

    val gpuConfiguration = FilterField(
        key = "gpu_configuration",
        label = "GPU Configuration",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Integrated Graphics Only (iGPU)", "Dedicated Graphics (dGPU)", "Other")
    )

    /** Only meaningful once a dGPU is actually in the picture — narrows [gpuModel] to just that brand's dGPU chips. */
    val gpuBrand = FilterField(
        key = "gpu_brand",
        label = "GPU Brand",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("NVIDIA", "AMD", "Intel", "Other"),
        visibleWhen = FieldDependency("gpu_configuration", setOf(DGPU_ID))
    )

    private val dgpuModelsByBrand: Map<String, List<FilterOption>> = mapOf(
        "nvidia" to options(
            "GeForce GTX 1050 Laptop GPU", "GeForce GTX 1050 Ti Laptop GPU", "GeForce GTX 1060 Laptop GPU",
            "GeForce GTX 1070 Laptop GPU", "GeForce GTX 1080 Laptop GPU", "GeForce GTX 1650 Laptop GPU",
            "GeForce GTX 1660 Ti Laptop GPU",
            "GeForce RTX 2060 Laptop GPU", "GeForce RTX 2070 Laptop GPU", "GeForce RTX 2080 Laptop GPU",
            "GeForce RTX 3050 Laptop GPU", "GeForce RTX 3050 Ti Laptop GPU", "GeForce RTX 3060 Laptop GPU",
            "GeForce RTX 3070 Laptop GPU", "GeForce RTX 3080 Laptop GPU",
            "GeForce RTX 4050 Laptop GPU", "GeForce RTX 4060 Laptop GPU", "GeForce RTX 4070 Laptop GPU",
            "GeForce RTX 4080 Laptop GPU", "GeForce RTX 4090 Laptop GPU",
            "GeForce RTX 5050 Laptop GPU", "GeForce RTX 5060 Laptop GPU", "GeForce RTX 5070 Laptop GPU",
            "GeForce RTX 5070 Ti Laptop GPU", "GeForce RTX 5080 Laptop GPU", "GeForce RTX 5090 Laptop GPU"
        ),
        "amd" to options(
            "Radeon RX 460 Mobile", "Radeon RX 560X", "Radeon RX Vega 56 Mobile",
            "Radeon RX 5500M", "Radeon RX 5600M", "Radeon RX 5700M",
            "Radeon RX 6600M", "Radeon RX 6700M", "Radeon RX 6800M",
            "Radeon RX 7600M", "Radeon RX 7600M XT", "Radeon RX 7700S"
        ),
        "intel" to options("Intel Arc A770M", "Intel Arc A730M", "Intel Arc A550M", "Intel Arc A370M"),
        "other" to emptyList()
    )

    private val igpuModels: List<FilterOption> = options(
        // iGPU (Intel, AMD, Apple, Qualcomm, 2016-2025)
        "Intel Arc Graphics 140V (Lunar Lake)", "Intel Arc Graphics 130V",
        "Intel Arc Graphics 8-Cores (Meteor Lake)", "Intel Arc Graphics 7-Cores",
        "Intel Iris Xe Graphics (96EU / 80EU)", "Intel Iris Plus Graphics",
        "Intel UHD Graphics", "Intel UHD Graphics 620",
        "AMD Radeon 890M (RDNA 3.5)", "AMD Radeon 880M", "AMD Radeon 780M",
        "AMD Radeon 760M", "AMD Radeon 680M",
        "AMD Radeon Vega 10 Graphics", "AMD Radeon Vega 8 Graphics",
        "Apple M4 Max 40-Core / 32-Core GPU", "Apple M4 Pro 20-Core / 16-Core GPU", "Apple M4 10-Core GPU",
        "Apple M3 Max 40-Core / 30-Core GPU", "Apple M3 Pro 18-Core / 14-Core GPU", "Apple M3 10-Core GPU",
        "Apple M2 Max / M2 Pro / M2 GPU", "Apple M1 Max / M1 Pro / M1 GPU",
        "Qualcomm Adreno GPU (Snapdragon X Elite / Plus)"
    )

    /** Narrows to the picked [gpuConfiguration] first (dGPU vs iGPU), then — for a dGPU — further to the picked [gpuBrand]'s chips, the same two-step narrowing [PhoneFilterSchema.socModel] introduced for SoC Brand/Model. */
    val gpuModel = FilterField(
        key = "gpu_model",
        label = "GPU Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = (dgpuModelsByBrand.values.flatten() + igpuModels).distinctBy { it.id },
        optionsForState = { state ->
            val selectedConfigIds = selectedIdsFor(state, "gpu_configuration")
            when {
                DGPU_ID in selectedConfigIds -> {
                    val selectedBrandIds = selectedIdsFor(state, "gpu_brand")
                    val matched = selectedBrandIds.flatMap { dgpuModelsByBrand[it] ?: emptyList() }.distinctBy { it.id }
                    matched.ifEmpty { dgpuModelsByBrand.values.flatten().distinctBy { it.id } }
                }
                "integrated_graphics_only_igpu" in selectedConfigIds -> igpuModels
                else -> (dgpuModelsByBrand.values.flatten() + igpuModels).distinctBy { it.id }
            }
        }
    )

    val gpuPowerLimit = FilterField(
        key = "gpu_power_limit",
        label = "GPU Power Limit (TGP)",
        type = FilterType.NumberRange(min = 35f, max = 175f, step = 5f, unit = "W", unitIsPrefix = false),
        visibleWhen = FieldDependency("gpu_configuration", setOf(DGPU_ID))
    )

    // --- Memory (RAM) ---

    val memoryCapacity = FilterField(
        key = "memory_capacity",
        label = "Memory Capacity",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("8GB", "16GB", "24GB / 32GB", "48GB / 64GB", "96GB / 128GB+", "Other")
    )

    val memoryType = FilterField(
        key = "memory_type",
        label = "Memory Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "LPDDR5 / LPDDR5X (Soldered)", "DDR5 SO-DIMM (Slots)", "LPCAMM2 (Modular LPDDR5X)",
            "LPDDR4X / DDR4", "Unified Memory (Apple Silicon)", "Other"
        )
    )

    val upgradability = FilterField(
        key = "upgradability",
        label = "Upgradability",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "Dual SO-DIMM Slots (Fully Upgradable)", "1 Slot + On-board RAM (Semi-Upgradable)",
            "Fully Soldered (Non-upgradable)", "Other"
        )
    )

    // --- Storage (SSD) ---

    val installedCapacity = FilterField(
        key = "installed_capacity",
        label = "Installed Capacity",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("512GB", "1TB", "2TB", "4TB+", "Other")
    )

    val storageInterface = FilterField(
        key = "storage_interface",
        label = "Storage Interface",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("PCIe 5.0 NVMe M.2", "PCIe 4.0 NVMe M.2", "PCIe 3.0 NVMe M.2", "Soldered Storage", "Other")
    )

    val m2SlotsCount = FilterField(
        key = "m2_slots_count",
        label = "M.2 Slots Count",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("1 × M.2 Slot", "2 × M.2 Slots", "3 × M.2 Slots", "Other")
    )

    // --- I/O Ports & Connectivity ---

    val ports = FilterField(
        key = "ports",
        label = "Ports",
        type = FilterType.CheckboxList,
        options = options(
            "Thunderbolt 5", "Thunderbolt 4", "USB4 (40Gbps)", "Full-Featured USB-C (DisplayPort + Power Delivery)",
            "USB-A 3.2", "HDMI 2.1", "Full-size SD Card Reader", "MicroSD Card Reader",
            "RJ-45 Ethernet Port (2.5G / 1G)", "3.5mm Headphone Jack", "Other"
        )
    )

    /** Key stays `connectivity` so it reuses [WirelessAudioFields.bluetoothVersion]'s dependency wiring. */
    val wirelessConnectivity = FilterField(
        key = "connectivity",
        label = "Wireless Connectivity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Wi-Fi 7", "Wi-Fi 6E / Wi-Fi 6", "5G / 4G Cellular (Nano-SIM / eSIM)", "Bluetooth", "Other")
    )

    /** Same option set as [WirelessAudioFields.bluetoothVersion] — reused directly. */
    val bluetoothVersion = WirelessAudioFields.bluetoothVersion

    // --- Battery & Charging ---

    val batteryCapacity = FilterField(
        key = "battery_capacity_wh",
        label = "Battery Capacity",
        type = FilterType.NumberRange(min = 40f, max = 99.9f, step = 1f, unit = " Wh", unitIsPrefix = false)
    )

    val officialBatteryLife = FilterField(
        key = "official_battery_life",
        label = "Official Battery Life",
        type = FilterType.NumberRange(min = 4f, max = 22f, step = 1f, unit = " Hours", unitIsPrefix = false)
    )

    val chargingInterface = FilterField(
        key = "charging_interface",
        label = "Charging Interface",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("USB-C PD Charging (65W - 140W+ PD)", "Proprietary DC Barrel / Slim Tip", "Other")
    )

    // --- Chassis, Weight & Dimensions ---

    /** Kept as its own `weight_kg` key rather than the shared `weight` key other categories use — those compare against [com.example.gadgetmover.model.ProductSpecs.weightGrams] directly, and grams vs. kilograms would silently mismatch. See [CategoryFilterMatching] for the kg-aware conversion this key gets instead. */
    val weight = FilterField(
        key = "weight_kg",
        label = "Weight",
        type = FilterType.NumberRange(min = 0.5f, max = 3.5f, step = 0.1f, unit = " kg", unitIsPrefix = false)
    )

    val thickness = FilterField(
        key = "thickness",
        label = "Thickness",
        type = FilterType.NumberRange(min = 10f, max = 30f, step = 1f, unit = "mm", unitIsPrefix = false)
    )

    val chassisMaterial = FilterField(
        key = "chassis_material",
        label = "Chassis Material",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "All-Aluminum CNC", "Magnesium-Aluminum Alloy", "Carbon Fiber",
            "Polycarbonate / Plastic", "Hybrid (Metal Lid + Plastic Body)", "Other"
        )
    )

    // --- Keyboard & Trackpad ---

    val keyboardBacklight = FilterField(
        key = "keyboard_backlight",
        label = "Keyboard Backlight",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Per-Key RGB", "Zone RGB", "Single White Backlight", "Non-Backlit", "Other")
    )

    val trackpadType = FilterField(
        key = "trackpad_type",
        label = "Trackpad Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Haptic Feedback Glass Trackpad", "Traditional Mechanical Glass Trackpad",
            "Mylar Trackpad", "Pointing Stick (TrackPoint)", "Other"
        )
    )

    // --- Biometrics & Security ---

    val biometricsSecurity = FilterField(
        key = "biometrics_security",
        label = "Biometrics & Security",
        type = FilterType.CheckboxList,
        options = options(
            "Windows Hello Facial Recognition (IR Camera)", "Fingerprint Reader",
            "Physical Webcam Privacy Shutter", "TPM 2.0 / Microsoft Pluton", "Other"
        )
    )

    // --- Audio & Webcam ---

    val webcamResolution = FilterField(
        key = "webcam_resolution",
        label = "Webcam Resolution",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("1080p FHD", "1440p QHD", "720p HD", "Other")
    )

    val builtInSpeakers = FilterField(
        key = "built_in_speakers",
        label = "Built-in Speakers",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("2 Speakers", "4 Speakers", "6 Speakers", "Other")
    )

    val audioStandards = FilterField(
        key = "audio_standards",
        label = "Audio Standards",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Dolby Atmos", "DTS:X", "Hi-Res Audio", "Other")
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options("MUX Switch / Advanced Optimus (Display Direct)", "Dedicated Numeric Keypad (Numpad)")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            laptopCategory,
            primaryScreenSize, secondaryScreenSize, aspectRatio, panelType, resolution, refreshRate,
            srgbCoverage, dciP3Coverage, adobeRgbCoverage, peakBrightness, screenFeatures,
            cpuBrand, cpuModel, npuTops,
            gpuConfiguration, gpuBrand, gpuModel, gpuPowerLimit,
            memoryCapacity, memoryType, upgradability,
            installedCapacity, storageInterface, m2SlotsCount,
            ports, wirelessConnectivity, bluetoothVersion,
            batteryCapacity, officialBatteryLife, chargingInterface,
            weight, thickness, chassisMaterial,
            keyboardBacklight, trackpadType,
            biometricsSecurity,
            webcamResolution, builtInSpeakers, audioStandards,
            features
        )
    )
}

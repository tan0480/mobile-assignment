package com.example.gadgetmover.model.filter

/**
 * The Tablets advanced filter schema. Its rear-camera section reuses [CameraSystemFields]
 * (identical to [PhoneFilterSchema], per spec: "Follows the exact same modular schema as Phones"),
 * and its PPS/USB-PD and Bluetooth-version fields reuse [MobileDeviceSharedFields].
 */
object TabletFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Apple", "Samsung", "Huawei", "Xiaomi", "Lenovo", "Microsoft Surface", "Amazon Fire",
            "Google Pixel Tablet", "OPPO", "Honor", "vivo", "ASUS", "Other"
        )
    )

    // --- Body & Form Factor ---

    val frameMaterial = FilterField(
        key = "frame_material",
        label = "Frame Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Aluminum Alloy", "Magnesium Alloy", "Plastic / Polycarbonate", "Other")
    )

    val backCoverMaterial = FilterField(
        key = "back_cover_material",
        label = "Back Cover Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Aluminum Metal Unibody", "Glass Back", "Vegan Leather Back", "Plastic / Polycarbonate", "Other")
    )

    /** Shares its key with [MiceFilterSchema.weight], so it gets real [com.example.gadgetmover.model.ProductSpecs.weightGrams] matching for free. */
    val weight = FilterField(
        key = "weight",
        label = "Weight",
        type = FilterType.NumberRange(min = 200f, max = 1000f, step = 20f, unit = "g", unitIsPrefix = false)
    )

    val thickness = FilterField(
        key = "thickness",
        label = "Thickness",
        type = FilterType.NumberRange(min = 5.0f, max = 9.0f, step = 0.5f, unit = "mm", unitIsPrefix = false)
    )

    // --- Display & Screen ---

    val screenSize = FilterField(
        key = "screen_size",
        label = "Screen Size",
        type = FilterType.NumberRange(min = 8.0f, max = 14.6f, step = 0.2f, unit = "\"", unitIsPrefix = false)
    )

    val aspectRatio = FilterField(
        key = "aspect_ratio",
        label = "Aspect Ratio",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("16:10", "3:2", "4:3", "7:5", "Other")
    )

    val panelType = FilterField(
        key = "panel_type",
        label = "Panel Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Tandem OLED", "OLED", "AMOLED", "Mini-LED LCD",
            "IPS LCD", "E-Ink Monochrome", "E-Ink Color", "Other"
        )
    )

    val resolution = FilterField(
        key = "resolution",
        label = "Resolution",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "1080p / 1200p (FHD / WUXGA)", "2K / 2.5K (2560 × 1600 / 2880 × 1800)",
            "2.8K / 3K (2880 × 1920 / 3000 × 2000)", "3.2K / 4K (3200 × 2133 / 3840 × 2400)", "Other"
        )
    )

    val refreshRate = FilterField(
        key = "refresh_rate",
        label = "Refresh Rate",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("60Hz", "90Hz", "120Hz", "144Hz", "Other")
    )

    val peakBrightness = FilterField(
        key = "peak_brightness",
        label = "Peak Brightness",
        type = FilterType.NumberRange(min = 200f, max = 5000f, step = 100f, unit = " nits", unitIsPrefix = false)
    )

    val pwmDimmingFrequency = FilterField(
        key = "pwm_dimming_frequency",
        label = "PWM Dimming Frequency",
        type = FilterType.NumberRange(min = 120f, max = 4320f, step = 120f, unit = " Hz", unitIsPrefix = false)
    )

    val matteAntiGlare = FilterField(
        key = "matte_anti_glare",
        label = "Matte Anti-Glare / Paper-like Texture Glass",
        type = FilterType.RadioGroup,
        options = options("Yes", "No")
    )

    // --- Processor (SoC) & Platform ---

    val operatingSystem = FilterField(
        key = "operating_system",
        label = "Operating System",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("iPadOS", "Android (Tablet Optimized UI)", "HarmonyOS (HarmonyOS NEXT)", "Other")
    )

    private val osVersionsByOs: Map<String, List<FilterOption>> = mapOf(
        "ipados" to options("iPadOS 26", "iPadOS 18", "iPadOS 17", "Other"),
        "android_tablet_optimized_ui" to options("Android 16", "Android 15", "Android 14", "Android 13", "Other"),
        "harmonyos_harmonyos_next" to options("HarmonyOS NEXT 5.1", "HarmonyOS NEXT 5.0", "HarmonyOS 4", "HarmonyOS 3", "Other"),
        "other" to options("Other")
    )

    /** Styled and dynamically filtered the same way [socModel] narrows to [socBrand] — see [PhoneFilterSchema.osVersion]. */
    val osVersion = FilterField(
        key = "os_version",
        label = "OS Version",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = osVersionsByOs.values.flatten().distinctBy { it.id },
        optionsForState = { state ->
            val selectedOsIds = (state.valueFor("operating_system") as? FilterFieldValue.MultiSelect)?.selectedIds ?: emptySet()
            val matched = selectedOsIds.flatMap { osVersionsByOs[it] ?: emptyList() }.distinctBy { it.id }
            matched.ifEmpty { osVersionsByOs.values.flatten().distinctBy { it.id } }
        }
    )

    val socBrand = FilterField(
        key = "soc_brand",
        label = "SoC Brand",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Apple M-Series / A-Series", "Qualcomm Snapdragon", "MediaTek Dimensity", "Huawei Kirin", "Other")
    )

    private val socModelsByBrand: Map<String, List<FilterOption>> = mapOf(
        "apple_m_series_a_series" to options("Apple M4 / M3 / M2 / M1", "Apple A17 Pro / A16 / A15"),
        "qualcomm_snapdragon" to options("Snapdragon 8 Elite / 8 Gen 3 / 8 Gen 2 / 8s Gen 3"),
        "mediatek_dimensity" to options("Dimensity 9400 / 9300+ / 9300 / 9000"),
        "huawei_kirin" to options("Kirin 9000W / Kirin 9000S"),
        "other" to options("Other")
    )

    /** Narrows to just the picked [socBrand]'s chips, the same dependent-options mechanism [PhoneFilterSchema.socModel] introduced. */
    val socModel = FilterField(
        key = "soc_model",
        label = "SoC Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = socModelsByBrand.values.flatten().distinctBy { it.id },
        optionsForState = { state ->
            val selectedBrandIds = (state.valueFor("soc_brand") as? FilterFieldValue.MultiSelect)?.selectedIds ?: emptySet()
            val matched = selectedBrandIds.flatMap { socModelsByBrand[it] ?: emptyList() }.distinctBy { it.id }
            matched.ifEmpty { socModelsByBrand.values.flatten().distinctBy { it.id } }
        }
    )

    // --- Memory & Storage ---

    val ramCapacity = FilterField(
        key = "ram_capacity",
        label = "RAM Capacity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("4GB", "6GB", "8GB", "12GB", "16GB", "24GB+", "Other")
    )

    val internalStorage = FilterField(
        key = "internal_storage",
        label = "Internal Storage",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("32GB", "64GB", "128GB", "256GB", "512GB", "1TB", "2TB", "Other")
    )

    val storageExpansion = FilterField(
        key = "storage_expansion",
        label = "Expandable Storage Supported (MicroSD / NM Card slot)",
        type = FilterType.SwitchToggle(label = "Expandable Storage Supported (MicroSD / NM Card slot)")
    )

    // --- Stylus & Official Keyboard Ecosystem ---

    private val stylusDependency = FieldDependency("stylus_supported", setOf(MobileDeviceSharedFields.SUPPORTED_ID))

    val stylusSupported = MobileDeviceSharedFields.supportGate("stylus_supported", "Stylus (Active Pen) Support")

    val stylusChargingMethod = FilterField(
        key = "stylus_charging_method",
        label = "Stylus Charging Method",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Magnetic Wireless Charging on Tablet Edge", "USB-C Direct Cable Charging",
            "Battery-Free EMR (Wacom Protocol)", "Other"
        ),
        visibleWhen = stylusDependency
    )

    val stylusPressureSensitivity = FilterField(
        key = "stylus_pressure_sensitivity",
        label = "Stylus Pressure Sensitivity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("4096 Levels", "8192 Levels", "16384+ Levels"),
        visibleWhen = stylusDependency
    )

    val officialKeyboardEcosystem = FilterField(
        key = "official_keyboard_ecosystem",
        label = "Official Keyboard Ecosystem",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Magnetic Cantilever Keyboard with Trackpad", "Pogo-Pin Detachable Keyboard",
            "Bluetooth Keyboard Case", "Other"
        )
    )

    // --- Battery & Charging ---

    val batteryCapacity = FilterField(
        key = "battery_capacity",
        label = "Battery Capacity",
        type = FilterType.NumberRange(min = 5000f, max = 15000f, step = 500f, unit = "mAh", unitIsPrefix = false)
    )

    val wiredFastCharging = FilterField(
        key = "wired_fast_charging_max_wattage",
        label = "Wired Fast Charging",
        type = FilterType.NumberRange(min = 18f, max = 120f, step = 5f, unit = "W", unitIsPrefix = false)
    )

    val bypassChargingSupport = FilterField(
        key = "bypass_charging_support",
        label = "Bypass Charging Support",
        type = FilterType.SwitchToggle(label = "Bypass Charging Support")
    )

    val reverseWiredCharging = FilterField(
        key = "reverse_wired_charging",
        label = "Reverse Wired Charging (Power Bank Mode)",
        type = FilterType.SwitchToggle(label = "Reverse Wired Charging (Power Bank Mode)")
    )

    /** Tablet-local, shortened from the shared "Charger Included in Box" — mirrors [PhoneFilterSchema.chargerIncluded]/[PhoneFilterSchema.boxIncluded]. */
    val chargerIncluded = FilterField(
        key = "charger_included",
        label = "Charger Included",
        type = FilterType.RadioGroup,
        options = options("Yes", "No")
    )

    val boxIncluded = FilterField(
        key = "box_included",
        label = "Box Included",
        type = FilterType.RadioGroup,
        options = options("Yes", "No")
    )

    // --- Speakers ---

    private const val DUAL_SPEAKER_ID = "dual_speakers_stereo"
    private const val QUAD_SPEAKER_ID = "quad_speakers_4_chamber_stereo"
    private const val SIX_SPEAKER_ID = "6_speakers_array"
    private const val EIGHT_SPEAKER_ID = "8_speakers_array"

    val speakerSystemConfiguration = FilterField(
        key = "speaker_system_configuration",
        label = "Speaker System Configuration",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Dual Speakers (Stereo)", "Quad Speakers (4-Chamber Stereo)", "6 Speakers Array", "8 Speakers Array", "Other")
    )

    /** Same real component catalogue as [PhoneFilterSchema.speakerModelOptions] — the same supplier ecosystem builds both phone and tablet speakers. */
    private val speakerModelOptions = PhoneFilterSchema.speakerModelOptions

    private val dualSpeakerDependency = FieldDependency("speaker_system_configuration", setOf(DUAL_SPEAKER_ID))
    private val speakers1Through4Dependency = FieldDependency("speaker_system_configuration", setOf(QUAD_SPEAKER_ID, SIX_SPEAKER_ID, EIGHT_SPEAKER_ID))
    private val speakers5And6Dependency = FieldDependency("speaker_system_configuration", setOf(SIX_SPEAKER_ID, EIGHT_SPEAKER_ID))
    private val eightSpeakerDependency = FieldDependency("speaker_system_configuration", setOf(EIGHT_SPEAKER_ID))

    val speakerModelLeft = FilterField(
        key = "speaker_model_left", label = "Left Speaker Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = dualSpeakerDependency
    )
    val speakerModelRight = FilterField(
        key = "speaker_model_right", label = "Right Speaker Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = dualSpeakerDependency
    )
    val speakerModel1 = FilterField(
        key = "speaker_model_1", label = "Speaker 1 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = speakers1Through4Dependency
    )
    val speakerModel2 = FilterField(
        key = "speaker_model_2", label = "Speaker 2 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = speakers1Through4Dependency
    )
    val speakerModel3 = FilterField(
        key = "speaker_model_3", label = "Speaker 3 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = speakers1Through4Dependency
    )
    val speakerModel4 = FilterField(
        key = "speaker_model_4", label = "Speaker 4 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = speakers1Through4Dependency
    )
    val speakerModel5 = FilterField(
        key = "speaker_model_5", label = "Speaker 5 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = speakers5And6Dependency
    )
    val speakerModel6 = FilterField(
        key = "speaker_model_6", label = "Speaker 6 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = speakers5And6Dependency
    )
    val speakerModel7 = FilterField(
        key = "speaker_model_7", label = "Speaker 7 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = eightSpeakerDependency
    )
    val speakerModel8 = FilterField(
        key = "speaker_model_8", label = "Speaker 8 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = eightSpeakerDependency
    )

    // --- Haptic Vibration Motor ---

    private const val X_AXIS_ID = "x_axis"
    private const val Z_AXIS_ID = "z_axis"

    /** Same Rotor/X-Axis/Z-Axis shape [PhoneFilterSchema.motorType] introduced, replacing the old flat "Motor Setup" + "Motor Type & Model" pair that mixed type and model together in one ungated chip list. */
    val motorType = FilterField(
        key = "motor_type",
        label = "Motor Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Rotor Motor", "X-Axis", "Z-Axis", "Other")
    )

    /** Same X-Axis component catalogue as [PhoneFilterSchema.motorModelXAxis]. */
    val motorModelXAxis = FilterField(
        key = "motor_model_x_axis", label = "Motor Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = PhoneFilterSchema.motorModelXAxis.options,
        visibleWhen = FieldDependency("motor_type", setOf(X_AXIS_ID))
    )

    /** Same Z-Axis component catalogue as [PhoneFilterSchema.motorModelZAxis]. */
    val motorModelZAxis = FilterField(
        key = "motor_model_z_axis", label = "Motor Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = PhoneFilterSchema.motorModelZAxis.options,
        visibleWhen = FieldDependency("motor_type", setOf(Z_AXIS_ID))
    )

    // --- Connectivity ---

    val network = FilterField(
        key = "network",
        label = "Network",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Wi-Fi Only", "5G Cellular", "5G-A Cellular", "4G LTE", "Other")
    )

    val wifiStandard = FilterField(
        key = "wifi_standard",
        label = "Wi-Fi Standard",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Wi-Fi 7", "Wi-Fi 6E", "Wi-Fi 6", "Wi-Fi 5", "Other")
    )

    val usbCSpecification = FilterField(
        key = "usb_c_specification",
        label = "USB-C Specification",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Thunderbolt 4 / USB4 (DisplayPort Out)", "USB 3.2 Gen 2 / Gen 1 (DisplayPort Out)",
            "USB 2.0", "Dual USB-C Ports", "Other"
        )
    )

    // --- Biometrics & Features ---

    val biometrics = FilterField(
        key = "biometrics",
        label = "Biometrics",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Face ID", "Side Fingerprint", "Under-Display Fingerprint", "Other")
    )

    val desktopMultiWindowMode = FilterField(
        key = "desktop_multi_window_mode",
        label = "Desktop / PC Multi-Window Mode (Samsung DeX, Stage Manager, PC Mode)",
        type = FilterType.SwitchToggle(label = "Desktop / PC Multi-Window Mode (Samsung DeX, Stage Manager, PC Mode)")
    )

    val hardwareAntiPeeping = FilterField(
        key = "hardware_anti_peeping",
        label = "Hardware Anti-Peeping Privacy Display",
        type = FilterType.SwitchToggle(label = "Hardware Anti-Peeping Privacy Display")
    )

    val ipRating = FilterField(
        key = "ip_rating",
        label = "Ingress Protection",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("IP68", "IP54", "Other")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            frameMaterial, backCoverMaterial, weight, thickness,
            screenSize, aspectRatio, panelType, resolution, refreshRate, peakBrightness, pwmDimmingFrequency, matteAntiGlare,
            operatingSystem, osVersion, socBrand, socModel,
            ramCapacity, internalStorage, storageExpansion
        ) + CameraSystemFields.fields + listOf(
            stylusSupported, stylusChargingMethod, stylusPressureSensitivity, officialKeyboardEcosystem,
            batteryCapacity, wiredFastCharging
        ) + MobileDeviceSharedFields.fastChargingProtocolFields + listOf(
            bypassChargingSupport, reverseWiredCharging, chargerIncluded, boxIncluded,
            speakerSystemConfiguration,
            speakerModelLeft, speakerModelRight,
            speakerModel1, speakerModel2, speakerModel3, speakerModel4,
            speakerModel5, speakerModel6, speakerModel7, speakerModel8,
            motorType, motorModelXAxis, motorModelZAxis,
            network, wifiStandard
        ) + MobileDeviceSharedFields.bluetoothFields + listOf(
            usbCSpecification, MobileDeviceSharedFields.headphoneJack,
            biometrics, desktopMultiWindowMode, hardwareAntiPeeping, ipRating
        )
    )
}

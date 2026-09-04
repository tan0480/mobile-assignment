package com.example.gadgetmover.model.filter

/**
 * The Smartphones advanced filter schema. Its rear-camera section reuses [CameraSystemFields]
 * (shared with [TabletFilterSchema], per spec), and its Bluetooth-version gating reuses
 * [MobileDeviceSharedFields]. PPS-only (no USB-PD) fast charging, per-Phone charger/box-included
 * fields, and the 3.5mm headphone jack (folded into [hardwareFeatures]) intentionally diverge from
 * [MobileDeviceSharedFields]'s shared defaults, so this schema keeps its own local copies of those
 * rather than the shared ones Tablets still use.
 */
object PhoneFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    private const val HORIZONTAL_FOLDABLE_ID = "horizontal_foldable"
    private const val VERTICAL_FOLDABLE_ID = "vertical_foldable"
    private const val TRI_FOLDABLE_ID = "tri_foldable"
    private val foldableDependency = FieldDependency(
        "form_factor", setOf(HORIZONTAL_FOLDABLE_ID, VERTICAL_FOLDABLE_ID, TRI_FOLDABLE_ID)
    )

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Apple", "Samsung", "Xiaomi", "Huawei", "OPPO", "vivo", "OnePlus", "Google Pixel", "Honor",
            "Sony", "Motorola", "Nothing", "Realme", "ASUS ROG Phone", "Sharp", "Meizu", "Other"
        )
    )

    // --- Form Factor & Body Design ---

    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Standard", "Horizontal Foldable", "Vertical Foldable", "Tri-Foldable", "Other")
    )

    val frameMaterial = FilterField(
        key = "frame_material",
        label = "Frame Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Titanium Alloy", "Aluminum Alloy", "Stainless Steel", "Plastic / Polycarbonate", "Other")
    )

    val backCoverMaterial = FilterField(
        key = "back_cover_material",
        label = "Back Cover Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Glass (AG Glass / Ceramic Glass)", "Ceramic", "Vegan Leather (PU Leather / Plain Leather)",
            "Fiberglass Composite", "Plastic / Polycarbonate", "Other"
        )
    )

    /** Shares its key with [MiceFilterSchema.weight], so it gets real [com.example.gadgetmover.model.ProductSpecs.weightGrams] matching for free. */
    val weight = FilterField(
        key = "weight",
        label = "Weight",
        type = FilterType.NumberRange(min = 100f, max = 500f, step = 10f, unit = "g", unitIsPrefix = false)
    )

    val thickness = FilterField(
        key = "thickness",
        label = "Thickness",
        type = FilterType.NumberRange(min = 5.0f, max = 16.0f, step = 0.5f, unit = "mm", unitIsPrefix = false)
    )

    // --- Display & Screen: Primary Display ---

    val screenSize = FilterField(
        key = "screen_size",
        label = "Screen Size",
        type = FilterType.NumberRange(min = 5.4f, max = 8.0f, step = 0.1f, unit = "\"", unitIsPrefix = false)
    )

    val panelType = FilterField(
        key = "panel_type",
        label = "Panel Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("LTPO OLED", "OLED", "AMOLED", "LCD", "Other")
    )

    val resolution = FilterField(
        key = "resolution",
        label = "Resolution",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("1080p (FHD / FHD+)", "1.5K (1220p / 1264p)", "2K (QHD+ / 1440p)", "4K (2160p)", "Other")
    )

    val refreshRate = FilterField(
        key = "refresh_rate",
        label = "Refresh Rate",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("60Hz", "90Hz", "120Hz", "144Hz", "165Hz", "Other")
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

    val screenGlassProtection = FilterField(
        key = "screen_glass_protection",
        label = "Screen Glass Protection",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Corning Gorilla Armor", "Corning Gorilla Glass Victus 2 / Victus", "Kunlun Glass", "Ceramic Shield", "Other"
        )
    )

    // --- Display & Screen: Cover / Secondary Display (foldables only) ---

    val coverScreenSize = FilterField(
        key = "cover_screen_size",
        label = "Cover / Secondary Screen Size",
        type = FilterType.NumberRange(min = 3.0f, max = 6.6f, step = 0.1f, unit = "\"", unitIsPrefix = false),
        visibleWhen = foldableDependency
    )

    val coverPanelType = FilterField(
        key = "cover_panel_type",
        label = "Cover Display Panel Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("LTPO OLED", "OLED", "AMOLED", "LCD", "Other"),
        visibleWhen = foldableDependency
    )

    val coverResolution = FilterField(
        key = "cover_resolution",
        label = "Cover Display Resolution",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("FHD / FHD+", "1.5K", "2K", "Other"),
        visibleWhen = foldableDependency
    )

    val coverRefreshRate = FilterField(
        key = "cover_refresh_rate",
        label = "Cover Display Refresh Rate",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("60Hz", "120Hz", "Other"),
        visibleWhen = foldableDependency
    )

    val coverPeakBrightness = FilterField(
        key = "cover_peak_brightness",
        label = "Cover Display Peak Brightness",
        type = FilterType.NumberRange(min = 200f, max = 5000f, step = 100f, unit = " nits", unitIsPrefix = false),
        visibleWhen = foldableDependency
    )

    val coverPwmDimmingFrequency = FilterField(
        key = "cover_pwm_dimming_frequency",
        label = "Cover Display PWM Dimming Frequency",
        type = FilterType.NumberRange(min = 120f, max = 4320f, step = 120f, unit = " Hz", unitIsPrefix = false),
        visibleWhen = foldableDependency
    )

    // --- Processor (SoC) & Platform ---

    val operatingSystem = FilterField(
        key = "operating_system",
        label = "Operating System",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Android", "iOS", "HarmonyOS (HarmonyOS NEXT)", "Other")
    )

    private val osVersionsByOs: Map<String, List<FilterOption>> = mapOf(
        "android" to options("Android 16", "Android 15", "Android 14", "Android 13", "Android 12", "Android 11", "Other"),
        "ios" to options("iOS 26", "iOS 18", "iOS 17", "iOS 16", "iOS 15", "Other"),
        "harmonyos_harmonyos_next" to options("HarmonyOS NEXT 5.1", "HarmonyOS NEXT 5.0", "HarmonyOS 4", "HarmonyOS 3", "Other"),
        "other" to options("Other")
    )

    /** Styled and dynamically filtered the same way [socModel] narrows to [socBrand]: pick an OS above and this narrows to just that OS's versions. */
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
        options = options(
            "Qualcomm Snapdragon", "Apple A-Series", "MediaTek Dimensity",
            "Google Tensor", "Huawei Kirin", "Samsung Exynos", "Other"
        )
    )

    private val socModelsByBrand: Map<String, List<FilterOption>> = mapOf(
        "qualcomm_snapdragon" to options(
            "Snapdragon 8 Elite", "Snapdragon 8 Gen 3 / 8 Gen 3 Leading Version",
            "Snapdragon 8s Gen 3", "Snapdragon 8 Gen 2",
            "Snapdragon 7+ Gen 3 / 7+ Gen 2", "Snapdragon 7 Gen 3"
        ),
        "apple_a_series" to options("Apple A18 Pro / A18", "Apple A17 Pro", "Apple A16 Bionic", "Apple A15 Bionic"),
        "mediatek_dimensity" to options(
            "Dimensity 9400", "Dimensity 9300+ / 9300", "Dimensity 9200+ / 9200",
            "Dimensity 8300-Ultra / 8300", "Dimensity 8200"
        ),
        "google_tensor" to options("Tensor G4 / G3"),
        "huawei_kirin" to options("Kirin 9010 / 9000S"),
        "samsung_exynos" to options("Exynos 2400"),
        "other" to options("Other")
    )

    /** Narrows to just the picked [socBrand]'s chips, e.g. selecting Apple here only ever pops up Apple's own SoC models. */
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
        options = options("4GB", "6GB", "8GB", "12GB", "16GB", "24GB", "Other")
    )

    val internalStorage = FilterField(
        key = "internal_storage",
        label = "Internal Storage (ROM)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("32GB", "64GB", "128GB", "256GB", "512GB", "1TB", "2TB", "Other")
    )

    // --- Camera Features & Co-Branding ---

    val cameraOpticsCoBranding = FilterField(
        key = "camera_optics_co_branding",
        label = "Camera Optics Co-branding",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Hasselblad", "Leica", "ZEISS", "XMAGE", "Other")
    )

    val cameraFeatures = FilterField(
        key = "camera_features",
        label = "Camera Features",
        type = FilterType.CheckboxList,
        options = options(
            "8K @ 60fps Video Recording", "4K @ 120fps Video Recording",
            "Live Photo Support", "Ultra HDR Display / Pro-XDR Support",
            "Audio Zoom Recording", "Dual-View Video Recording (Multi-cam)",
            "Variable / Continuous Optical Zoom", "Multi-step Optical Zoom",
            "Ultra-wide Macro Capable", "Telephoto Macro Capable", "Other"
        )
    )

    // --- Battery & Charging ---

    val batteryCapacity = FilterField(
        key = "battery_capacity",
        label = "Battery Capacity",
        type = FilterType.NumberRange(min = 2000f, max = 10000f, step = 200f, unit = "mAh", unitIsPrefix = false)
    )

    val wiredFastChargingMaxWattage = FilterField(
        key = "wired_fast_charging_max_wattage",
        label = "Wired Fast Charging (Phone Native Max Wattage)",
        type = FilterType.NumberRange(min = 18f, max = 240f, step = 5f, unit = "W", unitIsPrefix = false)
    )

    /** Phone-local: PPS is enough on its own, unlike [MobileDeviceSharedFields.fastChargingProtocolFields] which also lists USB-PD for Tablets. */
    val ppsSupportedWattage = FilterField(
        key = "pps_supported_wattage",
        label = "PPS Supported (Programmable Power Supply)",
        type = FilterType.NumberRange(min = 20f, max = 100f, step = 5f, unit = "W", unitIsPrefix = false)
    )

    private const val WIRELESS_CHARGING_SUPPORTED_ID = MobileDeviceSharedFields.SUPPORTED_ID
    private val wirelessChargingDependency = FieldDependency("wireless_charging_supported", setOf(WIRELESS_CHARGING_SUPPORTED_ID))

    val wirelessChargingSupported = MobileDeviceSharedFields.supportGate("wireless_charging_supported", "Wireless Charging")

    val wirelessChargingWattage = FilterField(
        key = "wireless_charging_wattage",
        label = "Wireless Charging Wattage",
        type = FilterType.NumberRange(min = 7.5f, max = 80f, step = 2.5f, unit = "W", unitIsPrefix = false),
        visibleWhen = wirelessChargingDependency
    )

    val magneticWirelessCharging = FilterField(
        key = "magnetic_wireless_charging",
        label = "Magnetic Wireless Charging (Qi2 / MagSafe)",
        type = FilterType.SwitchToggle(label = "Magnetic Wireless Charging (Qi2 / MagSafe)"),
        visibleWhen = wirelessChargingDependency
    )

    /** Phone-local, shortened from the shared "Charger Included in Box" — paired with [boxIncluded] as its own separate question instead of bundling both into one label. */
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

    private const val SINGLE_SPEAKER_ID = "single_speaker"
    private const val SYMMETRICAL_DUAL_ID = "symmetrical_dual_speaker"
    private const val ASYMMETRICAL_DUAL_ID = "asymmetrical_dual_speaker"
    private const val QUAD_SPEAKER_ID = "quad_speaker"

    val speakerSystemConfiguration = FilterField(
        key = "speaker_system_configuration",
        label = "Speaker System Configuration",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Single Speaker", "Symmetrical Dual Speaker", "Asymmetrical Dual Speaker", "Quad Speaker")
    )

    /** Shared with [TabletFilterSchema] — the same real speaker-component supplier ecosystem builds both. */
    val speakerModelOptions = options(
        "AAC SLS 1115", "AAC SLS 1115A", "AAC SLS 1115B", "AAC SLS 1115C", "AAC SLS 1115D", "AAC SLS 1115E",
        "AAC SLS 1115K", "AAC SLS 1115K Plus", "AAC SLS 1115X", "AAC SLS Master 1115",
        "AAC Coaxial 1115", "AAC Coaxial 1115 2.0", "AAC Dual-Diaphragm 1115",
        "AAC SLS 1216", "AAC SLS 1216A", "AAC SLS 1216B", "AAC SLS 1216C", "AAC SLS 1216D",
        "AAC SLS 1216K", "AAC SLS 1216K Plus", "AAC SLS Master 1216", "AAC SLS 1216X",
        "AAC SLS 1012", "AAC SLS 1012A", "AAC SLS 1012B", "AAC SLS 1012C", "AAC SLS 1012K",
        "AAC SLS 1014", "AAC SLS 1014A", "AAC SLS 1014B", "AAC SLS 1014K",
        "AAC SLS 1318", "AAC SLS 1318K", "AAC SLS 1318B", "AAC SLS 1620 Bass Unit",
        "AAC Ultra-Thin 0809", "AAC Ultra-Thin 0916", "AAC Ultra-Thin 1010",
        "Goertek Super Linear 1115", "Goertek Super Linear 1115A", "Goertek Super Linear 1115B",
        "Goertek Super Linear 1115D", "Goertek Super Linear 1115G", "Goertek Super Linear 1115 Pro",
        "Goertek Super Linear 1216", "Goertek Super Linear 1216A", "Goertek Super Linear 1216B",
        "Goertek Super Linear 1216D", "Goertek Super Linear 1216 Pro", "Goertek Super Linear 1216 Max",
        "Goertek Micro Receiver 1012", "Goertek Micro Receiver 1012A", "Goertek Micro Receiver 1012B",
        "Goertek Micro Receiver 1014", "Goertek Micro Receiver 1014A", "Goertek Micro Receiver 1014B",
        "Goertek Super Linear 1318", "Goertek Super Linear 1318A", "Goertek Subwoofer 1519", "Goertek Foldable 0914",
        "Knowles Cobra 1115", "Knowles Super Cobra 1115", "Knowles 1115 Receiver-Speaker",
        "Knowles 1216 High Output", "Knowles 1014 Dynamic Speaker", "Knowles 0916 Low Profile",
        "BestTechnic BES-SPK 1115", "BestTechnic BES-SPK 1216",
        "Foster Electric 1115 Micro Dynamic", "Foster Electric 1216 Micro Dynamic", "Foster Electric 1012 Receiver Hybrid",
        "Hosiden 1115 Ultra Linear", "Hosiden 1216 Ultra Linear", "Hosiden 1014 Slim Dynamic",
        "Other"
    )

    private val singleSpeakerDependency = FieldDependency("speaker_system_configuration", setOf(SINGLE_SPEAKER_ID))
    private val dualSpeakerDependency = FieldDependency("speaker_system_configuration", setOf(SYMMETRICAL_DUAL_ID, ASYMMETRICAL_DUAL_ID))
    private val quadSpeakerDependency = FieldDependency("speaker_system_configuration", setOf(QUAD_SPEAKER_ID))

    val speakerModel = FilterField(
        key = "speaker_model", label = "Speaker Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = singleSpeakerDependency
    )
    val speakerModelTop = FilterField(
        key = "speaker_model_top", label = "Top Speaker Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = dualSpeakerDependency
    )
    val speakerModelBottom = FilterField(
        key = "speaker_model_bottom", label = "Bottom Speaker Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = dualSpeakerDependency
    )
    val speakerModel1 = FilterField(
        key = "speaker_model_1", label = "Speaker 1 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = quadSpeakerDependency
    )
    val speakerModel2 = FilterField(
        key = "speaker_model_2", label = "Speaker 2 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = quadSpeakerDependency
    )
    val speakerModel3 = FilterField(
        key = "speaker_model_3", label = "Speaker 3 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = quadSpeakerDependency
    )
    val speakerModel4 = FilterField(
        key = "speaker_model_4", label = "Speaker 4 Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = speakerModelOptions, visibleWhen = quadSpeakerDependency
    )

    // --- Haptic Vibration Motor ---

    private const val X_AXIS_ID = "x_axis"
    private const val Z_AXIS_ID = "z_axis"

    val motorType = FilterField(
        key = "motor_type",
        label = "Motor Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Rotor Motor", "X-Axis", "Z-Axis", "Other")
    )

    val motorModelXAxis = FilterField(
        key = "motor_model_x_axis", label = "Motor Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "AAC CyberEngine 0916 (CSA 0916)", "AAC CyberEngine 0916 Turbo (CSA+ 0916)",
            "AAC CyberEngine Ultra 1016 (CSA 1016)", "AAC CyberEngine 1010 (CSA 1010)",
            "AAC SLA 0620", "AAC SLA 0620B", "AAC SLA 0815 (CSA 0815)", "AAC SLA 0815B", "AAC SLA 0815C", "AAC SLA 0815D",
            "AAC SLA 0914", "AAC SLA 0914B", "AAC SLA 0959 (AAC 9595)",
            "AAC ELA 0809", "AAC ELA 0809B", "AAC ELA 0809C", "AAC ELA 0809D", "AAC ELA 0809E",
            "AAC Ultra-Thin 0714", "AAC Ultra-Thin 0715",
            "Goertek LRA 0619", "Goertek LRA 0620", "Goertek LRA 0809", "Goertek LRA 0809A", "Goertek LRA 0809B", "Goertek LRA 0809C",
            "Goertek LRA 0815", "Goertek LRA 0815A", "Goertek LRA 0815B", "Goertek LRA 0815 Pro", "Goertek LRA 0815 Max",
            "Goertek LRA 0914", "Goertek LRA 0914A", "Goertek LRA 0916", "Goertek LRA 0916 Pro", "Goertek LRA 0959",
            "Goertek LRA 1010", "Goertek LRA 1016", "Goertek LRA 1016 Pro", "Goertek Ultra-Linear 1016",
            "Nidec Sprinter X 0619", "Nidec Sprinter X 0809", "Nidec Sprinter X 0809B", "Nidec Sprinter X 0815", "Nidec Sprinter X 0815B",
            "Nidec Sprinter X 0914", "Nidec Sprinter X 0916", "Nidec Sprinter X 1010", "Nidec Sprinter X 1016", "Nidec Sprinter X 1016 Pro",
            "Nidec Ultra Haptic 0815", "Nidec Ultra Haptic 1016",
            "Bluecom LRA 0809", "Bluecom LRA 0815", "Bluecom LRA 0916",
            "Johnson Electric X-Axis LRA 0815", "Johnson Electric X-Axis LRA 0916",
            "Apple Taptic Engine (Custom X-Axis LRA 1206)", "Apple Taptic Engine (Custom X-Axis LRA 1307)",
            "Apple Taptic Engine (Custom X-Axis LRA 1408)", "Apple Taptic Engine (Custom X-Axis LRA 1508)",
            "Other"
        ),
        visibleWhen = FieldDependency("motor_type", setOf(X_AXIS_ID))
    )

    val motorModelZAxis = FilterField(
        key = "motor_model_z_axis", label = "Motor Model",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "AAC Z-Axis LRA 0825", "AAC Z-Axis LRA 0830", "AAC Z-Axis LRA 0832", "AAC Z-Axis LRA 0832B",
            "AAC Z-Axis LRA 1027", "AAC Z-Axis LRA 1030", "AAC Z-Axis LRA 1034",
            "Goertek Z-Axis 0825", "Goertek Z-Axis 0832", "Goertek Z-Axis 1027", "Goertek Z-Axis 1030",
            "Nidec Sprinter D 0825", "Nidec Sprinter D 0832", "Nidec Sprinter D 0834",
            "Nidec Sprinter A 1027", "Nidec Sprinter A 1030",
            "Bluecom Z-Axis 0832", "Bluecom Z-Axis 1027", "Johnson Electric Z-Axis LRA 0832",
            "Other"
        ),
        visibleWhen = FieldDependency("motor_type", setOf(Z_AXIS_ID))
    )

    // --- Cellular & Wireless Connectivity ---

    val network = FilterField(
        key = "network",
        label = "Network",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("5G", "5G-A", "4G LTE", "Other")
    )

    val simSlots = FilterField(
        key = "sim_slots",
        label = "SIM Slots",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Dual Physical Nano-SIM", "Single Nano-SIM + eSIM", "Dual eSIM", "Other")
    )

    val satelliteConnectivity = FilterField(
        key = "satellite_connectivity",
        label = "Satellite Connectivity",
        type = FilterType.CheckboxList,
        options = options(
            "Emergency SOS via Satellite", "Satellite Messaging", "Satellite Image Messaging", "Satellite Voice Call"
        )
    )

    val wifiStandard = FilterField(
        key = "wifi_standard",
        label = "Wi-Fi Standard",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Wi-Fi 7", "Wi-Fi 6E", "Wi-Fi 6", "Wi-Fi 5", "Other")
    )

    val usbCInterface = FilterField(
        key = "usb_c_interface",
        label = "USB-C Interface",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "USB 3.2 Gen 2 (10Gbps / DisplayPort Out)", "USB 3.2 Gen 1 (5Gbps)", "USB 2.0 (480Mbps)", "Other"
        )
    )

    // --- Biometrics & Ingress Protection ---

    val fingerprintScanner = FilterField(
        key = "fingerprint_scanner",
        label = "Fingerprint Scanner",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "3D Ultrasonic Single-Point Under-Display", "3D Ultrasonic Wide-Area Under-Display",
            "Ultra-Thin Optical Under-Display", "Short-Focus Optical Under-Display",
            "Side-Mounted Physical Fingerprint", "Other"
        )
    )

    val facialRecognition = FilterField(
        key = "facial_recognition",
        label = "Facial Recognition",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("3D Structured Light", "ToF", "2D Camera", "Other")
    )

    val ipRating = FilterField(
        key = "ip_rating",
        label = "IP Rating",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("IP69", "IP68", "IP65", "IP54", "Other")
    )

    // --- Hardware Features ---

    val hardwareFeatures = FilterField(
        key = "hardware_features",
        label = "Hardware Features",
        type = FilterType.CheckboxList,
        options = options(
            "3.5mm Headphone Jack", "Reverse Wireless Charging Support", "Expandable Storage Supported", "Dual Motor",
            "Under-Display Front Camera",
            "Bypass Charging", "Built-in Privacy Screen Filter", "Infrared Blaster", "Full-Featured NFC",
            "Physical Alert Slider / Custom Action Button", "Dedicated Camera Shutter Key", "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            formFactor, frameMaterial, backCoverMaterial, weight, thickness,
            screenSize, panelType, resolution, refreshRate, peakBrightness, pwmDimmingFrequency, screenGlassProtection,
            coverScreenSize, coverPanelType, coverResolution, coverRefreshRate, coverPeakBrightness, coverPwmDimmingFrequency,
            operatingSystem, osVersion, socBrand, socModel,
            ramCapacity, internalStorage
        ) + CameraSystemFields.fields + listOf(
            cameraOpticsCoBranding, cameraFeatures,
            batteryCapacity, wiredFastChargingMaxWattage, ppsSupportedWattage,
            wirelessChargingSupported, wirelessChargingWattage, magneticWirelessCharging,
            chargerIncluded, boxIncluded,
            speakerSystemConfiguration, speakerModel, speakerModelTop, speakerModelBottom,
            speakerModel1, speakerModel2, speakerModel3, speakerModel4,
            motorType, motorModelXAxis, motorModelZAxis,
            network, simSlots, satelliteConnectivity, wifiStandard
        ) + MobileDeviceSharedFields.bluetoothFields + listOf(
            usbCInterface,
            fingerprintScanner, facialRecognition, ipRating,
            hardwareFeatures
        )
    )
}

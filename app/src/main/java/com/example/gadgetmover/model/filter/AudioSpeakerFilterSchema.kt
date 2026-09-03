package com.example.gadgetmover.model.filter

/**
 * The Audio & Speakers advanced filter schema — the deepest-nested schema so far. "Acoustic
 * Architecture & Driver Setup" is a chain of dependent fields: Speaker Configuration gates which
 * of Full-Range/Tweeter/Midrange/Woofer setups appear, each via its own [FieldDependency] on the
 * same `speaker_configuration` field (our dependency model is single-field/any-of, which covers
 * every branch here since nothing depends on two different fields at once).
 */
object AudioSpeakerFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    private const val SMART_SPEAKER_ID = "smart_speaker_voice_assistant"
    private const val CONFIG_FULL_RANGE_ID = "full_range"
    private const val CONFIG_2WAY_ID = "2_way_tweeter_woofer"
    private const val CONFIG_3WAY_ID = "3_way_tweeter_midrange_woofer"
    private const val CONFIG_SUBWOOFER_ID = "2_1_subwoofer_system"
    private const val BUILTIN_BATTERY_ID = "built_in_rechargeable_lithium_battery"
    private const val WIFI_ID = "wi_fi_network_streaming"

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Sonos", "JBL", "Bose", "Sony", "Bang & Olufsen", "Marshall", "Harman Kardon", "Anker Soundcore",
            "Ultimate Ears (UE)", "Klipsch", "Yamaha", "Denon", "Polk Audio", "KEF", "Edifier", "Logitech",
            "Devialet", "Naim", "Bowers & Wilkins", "Amazon Echo", "Google Nest", "Xiaomi", "Other"
        )
    )

    val speakerType = FilterField(
        key = "speaker_type",
        label = "Speaker Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Bookshelf Speakers", "Monitor Speakers", "Floorstanding / Tower Speakers", "Soundbar",
            "Portable Bluetooth Speaker", "Outdoor Speaker", "In-Wall / Ceiling Speaker",
            "Smart Speaker (Voice Assistant)", "PA Speaker", "Other"
        )
    )

    val voiceAssistant = FilterField(
        key = "voice_assistant",
        label = "Voice Assistant",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Alexa", "Google Assistant", "Siri", "Xiao AI (小米小爱同学)", "Xiaodu (百度小度)",
            "Tmall Genie (天猫精灵)", "Celia / Xiaoyi (华为小艺)", "Other"
        ),
        visibleWhen = FieldDependency("speaker_type", setOf(SMART_SPEAKER_ID))
    )

    val amplificationPowerType = FilterField(
        key = "amplification_power_type",
        label = "Amplification & Power Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Active / Powered (Built-in Amplifier)", "Passive (External Amp Required)", "Other")
    )

    val totalOutputPower = FilterField(
        key = "total_output_power",
        label = "Total Output Power (RMS)",
        type = FilterType.NumberRange(min = 5f, max = 500f, step = 5f, unit = " W RMS", unitIsPrefix = false)
    )

    // --- Acoustic Architecture & Driver Setup ---

    val speakerConfiguration = FilterField(
        key = "speaker_configuration",
        label = "Speaker Configuration",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Full-Range", "2-Way (Tweeter + Woofer)", "3-Way (Tweeter + Midrange + Woofer)",
            "2.1 / Subwoofer System", "Coaxial Driver", "Other"
        )
    )

    val fullRangeDriverSize = FilterField(
        key = "full_range_driver_size",
        label = "Full-Range Driver Size",
        type = FilterType.NumberRange(min = 2f, max = 8f, step = 0.5f, unit = "\"", unitIsPrefix = false),
        visibleWhen = FieldDependency("speaker_configuration", setOf(CONFIG_FULL_RANGE_ID))
    )

    val fullRangeDriverMaterial = FilterField(
        key = "full_range_driver_material",
        label = "Full-Range Driver Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Paper / Treated Paper", "Polypropylene (PP)", "Aluminum / Magnesium Alloy",
            "Kevlar", "Carbon Fiber", "Other"
        ),
        visibleWhen = FieldDependency("speaker_configuration", setOf(CONFIG_FULL_RANGE_ID))
    )

    private val tweeterDependency = FieldDependency("speaker_configuration", setOf(CONFIG_2WAY_ID, CONFIG_3WAY_ID))

    val tweeterType = FilterField(
        key = "tweeter_type",
        label = "Tweeter Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Dome", "Ribbon", "Horn", "AMT (Air Motion Transformer)", "Planar", "Other"),
        visibleWhen = tweeterDependency
    )

    val tweeterMaterial = FilterField(
        key = "tweeter_material",
        label = "Tweeter Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Silk", "Polymer", "Aluminum", "Titanium", "Beryllium", "Diamond", "Ceramic", "Other"),
        visibleWhen = tweeterDependency
    )

    val tweeterSize = FilterField(
        key = "tweeter_size",
        label = "Tweeter Size",
        type = FilterType.NumberRange(min = 10f, max = 50f, step = 2f, unit = "mm", unitIsPrefix = false),
        visibleWhen = tweeterDependency
    )

    private val midrangeDependency = FieldDependency("speaker_configuration", setOf(CONFIG_3WAY_ID))

    val midrangeType = FilterField(
        key = "midrange_type",
        label = "Midrange Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Cone", "Dome", "Planar", "Other"),
        visibleWhen = midrangeDependency
    )

    val midrangeMaterial = FilterField(
        key = "midrange_material",
        label = "Midrange Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Paper / Treated Paper", "Aluminum", "Kevlar", "Carbon Fiber", "Beryllium", "Ceramic", "Other"),
        visibleWhen = midrangeDependency
    )

    val midrangeSize = FilterField(
        key = "midrange_size",
        label = "Midrange Size",
        type = FilterType.NumberRange(min = 2f, max = 8f, step = 0.5f, unit = "\"", unitIsPrefix = false),
        visibleWhen = midrangeDependency
    )

    private val wooferDependency = FieldDependency("speaker_configuration", setOf(CONFIG_2WAY_ID, CONFIG_3WAY_ID, CONFIG_SUBWOOFER_ID))

    val wooferMaterial = FilterField(
        key = "woofer_material",
        label = "Woofer Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Paper / Treated Paper", "Polypropylene (PP)", "Kevlar", "Carbon Fiber",
            "Aluminum", "Magnesium", "Composite", "Other"
        ),
        visibleWhen = wooferDependency
    )

    val wooferSize = FilterField(
        key = "woofer_size",
        label = "Woofer Size",
        type = FilterType.NumberRange(min = 3f, max = 15f, step = 0.5f, unit = "\"", unitIsPrefix = false),
        visibleWhen = wooferDependency
    )

    val additionalBassEnhancement = FilterField(
        key = "additional_bass_enhancement",
        label = "Additional Bass Enhancement",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Passive Radiator (Drone Cone)", "Bass Reflex Port (Front/Rear Vented)",
            "Sealed Box (Acoustic Suspension)", "Other"
        )
    )

    // --- Audio Specifications ---

    val frequencyResponseLow = FilterField(
        key = "frequency_response_low",
        label = "Frequency Response (Lowest)",
        type = FilterType.NumberRange(min = 20f, max = 80f, step = 5f, unit = " Hz", unitIsPrefix = false)
    )

    val frequencyResponseHigh = FilterField(
        key = "frequency_response_high",
        label = "Frequency Response (Highest)",
        type = FilterType.NumberRange(min = 20000f, max = 40000f, step = 1000f, unit = " Hz", unitIsPrefix = false)
    )

    val signalToNoiseRatio = FilterField(
        key = "signal_to_noise_ratio",
        label = "Signal-to-Noise Ratio (SNR)",
        type = FilterType.NumberRange(min = 80f, max = 110f, step = 1f, unit = " dB", unitIsPrefix = false)
    )

    // --- Inputs & Connectivity ---

    val wiredInputs = FilterField(
        key = "wired_inputs",
        label = "Wired Inputs",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "3.5mm AUX", "RCA (Stereo)", "Optical (Toslink)", "Coaxial", "USB-DAC / USB-Audio (Type-C / Type-B)",
            "HDMI ARC / eARC", "Balanced XLR", "Balanced 6.35mm TRS", "Phono (For Vinyl Turntable)", "Other"
        )
    )

    /** Wireless-only, kept separate from [wiredInputs]; key stays `connectivity` so it reuses [WirelessAudioFields.bluetoothVersion]'s dependency wiring. */
    val wirelessConnectivity = FilterField(
        key = "connectivity",
        label = "Wireless Connectivity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Bluetooth", "Wi-Fi (Network Streaming)", "Other")
    )

    /** Same option set as [WirelessAudioFields.bluetoothVersion] — reused directly. */
    val bluetoothVersion = WirelessAudioFields.bluetoothVersion

    /** Speakers don't have a NearLink connectivity option, so unlike [WirelessAudioFields.wirelessCodecs] this omits NearLink Audio / 2.4GHz Proprietary and is Bluetooth-only. */
    val wirelessCodecs = FilterField(
        key = "wireless_codecs",
        label = "Wireless Audio Codecs & Protocols",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "SBC", "AAC", "Qualcomm aptX", "Qualcomm aptX HD", "Qualcomm aptX Adaptive", "Qualcomm aptX Lossless",
            "Sony LDAC", "LHDC", "Huawei L2HC", "LC3 / LE Audio", "Other"
        ),
        visibleWhen = FieldDependency("connectivity", setOf(WirelessAudioFields.BLUETOOTH_ID))
    )

    val networkStreamingProtocols = FilterField(
        key = "network_streaming_protocols",
        label = "Network Streaming Protocols",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Apple AirPlay 2", "Spotify Connect", "TIDAL Connect", "DLNA / UPnP",
            "Roon Ready / Roon Tested", "Chromecast Built-in", "Other"
        ),
        visibleWhen = FieldDependency("connectivity", setOf(WIFI_ID))
    )

    val audioTechnology = FilterField(
        key = "audio_technology",
        label = "Audio Technology & Surround Decoding",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Dolby Atmos", "Dolby Digital / TrueHD", "DTS:X", "DTS-HD Master Audio",
            "Spatial Audio", "DSP Room Correction", "Other"
        )
    )

    val audioCertifications = FilterField(
        key = "audio_certifications",
        label = "Audio Certifications",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Hi-Res Audio (Wired)", "Hi-Res Audio Wireless", "HWA", "THX Certified", "Other")
    )

    // --- Power Supply & Battery ---

    val powerSupplyMode = FilterField(
        key = "power_supply_mode",
        label = "Power Supply Mode",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "AC Wall Power (Mains Direct)", "Built-in Rechargeable Lithium Battery",
            "DC Adapter Powered", "USB Powered (Bus-powered)", "Other"
        )
    )

    val batteryCapacity = FilterField(
        key = "battery_capacity",
        label = "Battery Capacity",
        type = FilterType.NumberRange(min = 1500f, max = 20000f, step = 500f, unit = "mAh", unitIsPrefix = false),
        visibleWhen = FieldDependency("power_supply_mode", setOf(BUILTIN_BATTERY_ID))
    )

    val batteryLife = FilterField(
        key = "battery_life",
        label = "Battery Life",
        type = FilterType.NumberRange(min = 4f, max = 30f, step = 1f, unit = " Hours", unitIsPrefix = false)
    )

    val chargingInterface = FilterField(
        key = "charging_interface",
        label = "Charging Interface",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("USB-C", "Micro-USB", "DC Barrel Jack", "AC Power Cable Direct", "Other")
    )

    val ipRating = FilterField(
        key = "ip_rating",
        label = "Ingress Protection (IP Rating)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("IPX4", "IPX5", "IPX6", "IPX7", "IP67", "IP68", "Other")
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Multi-Room Audio Sync",
            "TWS Wireless Stereo Pairing (Link 2 Speakers)",
            "Party Cast / Multi-Speaker Linking",
            "Built-in Microphone (Speakerphone / Voice Calls)",
            "App EQ Customization",
            "RGB / Rhythm Ambient Lighting",
            "Power Bank Function (Reverse Phone Charging)",
            "Physical Remote Control Included",
            "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            speakerType, voiceAssistant,
            amplificationPowerType, totalOutputPower,
            speakerConfiguration,
            fullRangeDriverSize, fullRangeDriverMaterial,
            tweeterType, tweeterMaterial, tweeterSize,
            midrangeType, midrangeMaterial, midrangeSize,
            wooferMaterial, wooferSize,
            additionalBassEnhancement,
            frequencyResponseLow, frequencyResponseHigh, signalToNoiseRatio,
            wiredInputs,
            wirelessConnectivity, bluetoothVersion, wirelessCodecs, networkStreamingProtocols,
            audioTechnology, audioCertifications,
            powerSupplyMode, batteryCapacity, batteryLife, chargingInterface,
            ipRating,
            features
        )
    )
}

package com.example.gadgetmover.model.filter

/**
 * The Headphones advanced filter schema. Unlike Keyboards/Mice, Headphones' connectivity options
 * are wired-connector variants plus Bluetooth/2.4GHz rather than a small fixed set with per-mode
 * polling rates, so it defines its own `connectivity` field instead of reusing
 * [ConnectivityPollingFields] — [CategoryFilterMatching] still recognizes any "wired_*" id as
 * [com.example.gadgetmover.model.Connectivity.WIRED] and any "2_4ghz_*" id as WIRELESS_2_4G. Its
 * Bluetooth/wireless-codec/audio-tech/certification fields come from [WirelessAudioFields], shared
 * with [WirelessEarphoneFilterSchema].
 */
object HeadphoneFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Sony", "Bose", "Sennheiser", "Beyerdynamic", "Audio-Technica", "AKG", "Bang & Olufsen", "JBL",
            "Skullcandy", "Beats", "Jabra", "Anker Soundcore", "Focal", "HiFiMan", "Grado", "Philips",
            "Marshall", "Edifier", "1MORE", "Shure", "Corsair", "SteelSeries", "Razer", "HyperX", "ASUS ROG",
            "Logitech G", "Other"
        )
    )

    val acousticDesign = FilterField(
        key = "acoustic_design",
        label = "Acoustic Design",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Closed-Back", "Open-Back", "Semi-Open", "Other")
    )

    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Over-Ear (Circumaural)", "On-Ear (Supra-aural)", "Other")
    )

    val earpadMaterial = FilterField(
        key = "earpad_material",
        label = "Earpad Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Leather / Protein Leather", "Fabric / Mesh", "Velour", "Memory Foam",
            "Hybrid (Leather + Fabric/Velour)", "Other"
        )
    )

    val driverType = FilterField(
        key = "driver_type",
        label = "Driver Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Dynamic", "Planar Magnetic", "Electrostatic", "Balanced Armature", "Bone Conduction", "Other")
    )

    val driverSize = FilterField(
        key = "driver_size",
        label = "Driver Size",
        type = FilterType.NumberRange(min = 30f, max = 70f, step = 5f, unit = "mm", unitIsPrefix = false)
    )

    val connectivity = FilterField(
        key = "connectivity",
        label = "Connectivity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Wired 3.5mm Single-Ended", "Wired 6.35mm (1/4\") Single-Ended", "Wired 4.4mm Balanced",
            "Wired 2.5mm Balanced", "Wired XLR Balanced (4-Pin / Dual 3-Pin)", "Wired USB-C Audio",
            "Bluetooth", "NearLink", "2.4GHz Low-Latency Wireless", "Other"
        )
    )

    /** Bluetooth version / wireless codec / audio-tech / audio-certification fields are shared — see [WirelessAudioFields]. */
    val bluetoothVersion = WirelessAudioFields.bluetoothVersion
    val wirelessCodecs = WirelessAudioFields.wirelessCodecs
    val audioTechnology = WirelessAudioFields.audioTechnology
    val audioCertifications = WirelessAudioFields.audioCertifications

    val impedance = FilterField(
        key = "impedance",
        label = "Impedance",
        type = FilterType.NumberRange(min = 16f, max = 600f, step = 4f, unit = " Ω", unitIsPrefix = false)
    )

    val sensitivity = FilterField(
        key = "sensitivity",
        label = "Sensitivity",
        type = FilterType.NumberRange(min = 85f, max = 120f, step = 1f, unit = " dB", unitIsPrefix = false)
    )

    val frequencyResponseLow = FilterField(
        key = "frequency_response_low",
        label = "Frequency Response (Lowest)",
        type = FilterType.NumberRange(min = 5f, max = 20f, step = 1f, unit = " Hz", unitIsPrefix = false)
    )

    val frequencyResponseHigh = FilterField(
        key = "frequency_response_high",
        label = "Frequency Response (Highest)",
        type = FilterType.NumberRange(min = 20000f, max = 50000f, step = 1000f, unit = " Hz", unitIsPrefix = false)
    )

    val batteryCapacity = FilterField(
        key = "battery_capacity",
        label = "Battery Capacity",
        type = FilterType.NumberRange(min = 300f, max = 1500f, step = 50f, unit = "mAh", unitIsPrefix = false)
    )

    /** Shares its key with [MiceFilterSchema.weight], so it gets the same real [com.example.gadgetmover.model.ProductSpecs.weightGrams] matching for free. */
    val weight = FilterField(
        key = "weight",
        label = "Weight",
        type = FilterType.NumberRange(min = 150f, max = 600f, step = 10f, unit = "g", unitIsPrefix = false)
    )

    /** Sports/gym-oriented headphones are commonly IP-rated; added for consistency with [WirelessEarphoneFilterSchema] and [AudioSpeakerFilterSchema], which both already carry this field. */
    val ipRating = FilterField(
        key = "ip_rating",
        label = "Ingress Protection (IP Rating)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("IPX4", "IPX5", "IPX7", "IP54", "IP55", "IP68", "Other")
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Active Noise Cancellation (ANC)",
            "Transparency Mode",
            "Built-in / Detachable Boom Microphone",
            "Multi-device Pairing (Multipoint)",
            "Foldable Design",
            "Detachable Cable",
            "Replaceable Earpads",
            "Spatial Audio / Head Tracking Support",
            "RGB Lighting",
            "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            acousticDesign, formFactor, earpadMaterial,
            driverType, driverSize,
            connectivity, bluetoothVersion, wirelessCodecs,
            impedance, sensitivity, frequencyResponseLow, frequencyResponseHigh,
            audioTechnology, audioCertifications,
            batteryCapacity, weight, ipRating,
            features
        )
    )
}

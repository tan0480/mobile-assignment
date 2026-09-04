package com.example.gadgetmover.model.filter

/**
 * The Wireless Earphones (TWS / Open-Ear) advanced filter schema. Its Bluetooth version, wireless
 * codec, audio-tech, and audio-certification fields come from [WirelessAudioFields], shared with
 * [HeadphoneFilterSchema].
 */
object WirelessEarphoneFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Apple", "Samsung", "Sony", "Bose", "JBL", "Anker Soundcore", "Jabra", "Beats", "Google",
            "Huawei", "Xiaomi", "OPPO", "Nothing", "Skullcandy", "1MORE", "Edifier", "QCY", "Haylou",
            "Amazfit", "Razer", "SteelSeries", "Other"
        )
    )

    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "TWS In-Ear (With Silicone/Foam Tips)", "TWS Semi-In-Ear (Half In-Ear / Stem Style)",
            "Open-Ear (OWS Earhook)", "Open-Ear (OWS Ear-Clip)", "Bone Conduction (Neckband)",
            "Air Conduction (Neckband)", "Neckband In-Ear (Wireless)", "Other"
        )
    )

    val acousticDriverType = FilterField(
        key = "acoustic_driver_type",
        label = "Acoustic Driver Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Single Dynamic Driver", "Dual Dynamic Driver (Coaxial)", "Dynamic + Balanced Armature Hybrid",
            "Planar Magnetic", "Bone Conduction Transducer", "Piezoelectric Ceramic", "Other"
        )
    )

    val driverSize = FilterField(
        key = "driver_size",
        label = "Driver Size",
        type = FilterType.NumberRange(min = 6f, max = 18f, step = 1f, unit = "mm", unitIsPrefix = false)
    )

    val connectivity = FilterField(
        key = "connectivity",
        label = "Connectivity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Bluetooth", "NearLink", "2.4GHz Ultra-Low Latency Wireless (Dongle)", "Other")
    )

    /** Bluetooth version / wireless codec / audio-tech / audio-certification fields are shared — see [WirelessAudioFields]. */
    val bluetoothVersion = WirelessAudioFields.bluetoothVersion
    val wirelessCodecs = WirelessAudioFields.wirelessCodecs
    val audioTechnology = WirelessAudioFields.audioTechnology
    val audioCertifications = WirelessAudioFields.audioCertifications

    val noiseCancellation = FilterField(
        key = "noise_cancellation",
        label = "Noise Cancellation",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Active Noise Cancellation (ANC)", "Transparency Mode / Ambient Sound", "Wind Noise Reduction", "Other")
    )

    val singlePlaybackTime = FilterField(
        key = "single_playback_time",
        label = "Single Playback Time",
        type = FilterType.NumberRange(min = 4f, max = 15f, step = 1f, unit = " Hours", unitIsPrefix = false)
    )

    val totalPlaybackTime = FilterField(
        key = "total_playback_time",
        label = "Total Playback Time (With Case)",
        type = FilterType.NumberRange(min = 15f, max = 60f, step = 5f, unit = " Hours", unitIsPrefix = false)
    )

    val charging = FilterField(
        key = "charging",
        label = "Charging",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("USB-C", "Wireless Charging", "Magnetic Charging", "Other")
    )

    val ipRating = FilterField(
        key = "ip_rating",
        label = "Ingress Protection (IP Rating)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("IPX4", "IPX5", "IPX7", "IP54", "IP55", "IP68", "Other")
    )

    val singleEarbudWeight = FilterField(
        key = "single_earbud_weight",
        label = "Single Earbud Weight",
        type = FilterType.NumberRange(min = 3.5f, max = 12f, step = 0.5f, unit = "g", unitIsPrefix = false)
    )

    /** Shares its key with [MiceFilterSchema.weight], so it gets real [com.example.gadgetmover.model.ProductSpecs.weightGrams] matching for free — the "total weight" reading is the closer match to a single product-level weight spec. */
    val totalWeight = FilterField(
        key = "weight",
        label = "Total Weight (Earbuds + Case)",
        type = FilterType.NumberRange(min = 30f, max = 120f, step = 5f, unit = "g", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Call Noise Cancellation",
            "AI Noise Cancellation",
            "Multi-device Pairing (Multipoint)",
            "Standalone Voice Recording",
            "Left/Right Interchangeable",
            "Wear Detection",
            "Real-time Translation",
            "Touch Controls",
            "Force / Pinch Sensor Controls",
            "App EQ Customization",
            "Low Latency Gaming Mode",
            "Integrated Find My / Device Tracking",
            "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            formFactor, acousticDriverType, driverSize,
            connectivity, bluetoothVersion, wirelessCodecs,
            noiseCancellation,
            audioTechnology, audioCertifications,
            singlePlaybackTime, totalPlaybackTime,
            charging, ipRating,
            singleEarbudWeight, totalWeight,
            features
        )
    )
}

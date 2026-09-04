package com.example.gadgetmover.model.filter

/**
 * Bluetooth version, wireless codec, and general audio-tech/certification fields are identical
 * across every wireless-audio category (Headphones, Wireless Earphones) — factored out once so
 * their large option catalogues and `visibleWhen` wiring aren't duplicated per schema. A schema
 * reuses these by including them in its own `connectivity` field's option list, using [BLUETOOTH_ID]
 * / [NEARLINK_ID] as the option ids so [bluetoothVersion]/[wirelessCodecs]'s dependencies resolve.
 */
object WirelessAudioFields {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    const val BLUETOOTH_ID = "bluetooth"
    const val NEARLINK_ID = "nearlink"

    /** Only meaningful once a Bluetooth-family connectivity mode is selected. */
    val bluetoothVersion = FilterField(
        key = "bluetooth_version",
        label = "Bluetooth",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("5.0", "5.1", "5.2", "5.3", "5.4", "6.0+", "Other"),
        visibleWhen = FieldDependency("connectivity", setOf(BLUETOOTH_ID))
    )

    /** Only meaningful once Bluetooth or NearLink is selected — both carry a negotiated wireless audio codec. */
    val wirelessCodecs = FilterField(
        key = "wireless_codecs",
        label = "Wireless Audio Codecs & Protocols",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "SBC", "AAC", "Qualcomm aptX", "Qualcomm aptX HD", "Qualcomm aptX Adaptive", "Qualcomm aptX Lossless",
            "Sony LDAC", "LHDC", "Huawei L2HC", "NearLink Audio", "LC3 / LE Audio",
            "2.4GHz Proprietary Low Latency", "Other"
        ),
        visibleWhen = FieldDependency("connectivity", setOf(BLUETOOTH_ID, NEARLINK_ID))
    )

    val audioTechnology = FilterField(
        key = "audio_technology",
        label = "Audio Technology",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Dolby Atmos", "DTS Headphone:X", "Spatial Audio", "360 Reality Audio", "Other")
    )

    val audioCertifications = FilterField(
        key = "audio_certifications",
        label = "Audio Certifications",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Hi-Res Audio", "Hi-Res Audio Wireless", "THX Certified", "Snapdragon Sound", "Other")
    )
}

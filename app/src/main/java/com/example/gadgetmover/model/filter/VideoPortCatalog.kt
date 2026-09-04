package com.example.gadgetmover.model.filter

/**
 * The port-type catalogue behind [MonitorFilterSchema.videoPorts] — replaces the old fixed
 * HDMI 2.1/2.0 and DisplayPort 2.1/1.4a count fields, plus the flat "Other Video Interfaces"
 * checklist, with one flat list a seller/buyer picks per port added via [FilterType.VideoPortBuilder].
 */
object VideoPortCatalog {
    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val portTypes: List<FilterOption> = options(
        "DisplayPort 1.2", "DisplayPort 1.4", "DisplayPort 1.4a", "DisplayPort 2.0", "DisplayPort 2.1",
        "Mini DisplayPort",
        "HDMI 1.4", "HDMI 2.0", "HDMI 2.0b", "HDMI 2.1", "HDMI 2.1a", "Mini HDMI", "Micro HDMI",
        "USB-C (DP Alt Mode)", "Thunderbolt 3", "Thunderbolt 4", "Thunderbolt 5",
        "DVI-D", "DVI-I", "VGA (D-Sub)"
    )
}

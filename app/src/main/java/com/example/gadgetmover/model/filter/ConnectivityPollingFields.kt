package com.example.gadgetmover.model.filter

/**
 * Connectivity + polling rate share the same shape across every category that has them (currently
 * Keyboards and Mice): one Connectivity chip group, then one polling-rate field per mode that only
 * appears once that mode is selected, since wired/2.4GHz/Bluetooth each top out at different rates.
 */
object ConnectivityPollingFields {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    const val WIRED_ID = "wired_type_c_usb"
    const val WIRELESS_2_4G_ID = "2_4ghz_wireless"
    const val BLUETOOTH_ID = "bluetooth"
    const val NEARLINK_ID = "nearlink"

    val connectivity = FilterField(
        key = "connectivity",
        label = "Connectivity",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Wired (Type-C / USB)", "2.4GHz Wireless", "Bluetooth", "NearLink", "Other")
    )

    val pollingRateWired = FilterField(
        key = "polling_rate_wired",
        label = "Polling Rate (Wired)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("125Hz", "250Hz", "500Hz", "1000Hz", "2000Hz", "4000Hz", "8000Hz", "Other"),
        visibleWhen = FieldDependency("connectivity", setOf(WIRED_ID))
    )

    val pollingRateWireless2_4G = FilterField(
        key = "polling_rate_2_4ghz",
        label = "Polling Rate (2.4GHz Wireless)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("125Hz", "250Hz", "500Hz", "1000Hz", "2000Hz", "4000Hz", "8000Hz", "Other"),
        visibleWhen = FieldDependency("connectivity", setOf(WIRELESS_2_4G_ID))
    )

    val pollingRateBluetooth = FilterField(
        key = "polling_rate_bluetooth",
        label = "Polling Rate (Bluetooth)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("125Hz", "133Hz", "250Hz", "Other"),
        visibleWhen = FieldDependency("connectivity", setOf(BLUETOOTH_ID))
    )

    val pollingRateNearLink = FilterField(
        key = "polling_rate_nearlink",
        label = "Polling Rate (NearLink)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("125Hz", "250Hz", "500Hz", "1000Hz", "2000Hz", "4000Hz", "8000Hz", "Other"),
        visibleWhen = FieldDependency("connectivity", setOf(NEARLINK_ID))
    )

    val fields: List<FilterField> = listOf(
        connectivity, pollingRateWired, pollingRateWireless2_4G, pollingRateBluetooth, pollingRateNearLink
    )
}

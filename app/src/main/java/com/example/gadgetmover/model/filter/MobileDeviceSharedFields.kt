package com.example.gadgetmover.model.filter

/**
 * Fields worded identically in the Phones and Tablets specs — "Universal Protocol Fast Charging
 * (PPS / PD)" and Bluetooth-version gating — factored out once rather than duplicated per schema.
 */
object MobileDeviceSharedFields {

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = it.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_'), label = it) }

    const val SUPPORTED_ID = "supported"

    val ppsSupportedWattage = FilterField(
        key = "pps_supported_wattage",
        label = "PPS Supported (Programmable Power Supply)",
        type = FilterType.NumberRange(min = 20f, max = 100f, step = 5f, unit = "W", unitIsPrefix = false)
    )

    val usbPdSupportedWattage = FilterField(
        key = "usb_pd_supported_wattage",
        label = "USB-PD Standard Supported",
        type = FilterType.NumberRange(min = 18f, max = 100f, step = 2f, unit = "W", unitIsPrefix = false)
    )

    val fastChargingProtocolFields: List<FilterField> = listOf(ppsSupportedWattage, usbPdSupportedWattage)

    /** A single-option chip group used as a togglable "is this supported" gate for [FieldDependency]s that need one — the shared [isVisible] mechanism only reads MultiSelect/SingleSelect values, not [FilterFieldValue.Toggle]. */
    fun supportGate(key: String, label: String): FilterField = FilterField(
        key = key,
        label = label,
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Supported")
    )

    val bluetoothSupported = supportGate("bluetooth_supported", "Bluetooth")

    val bluetoothVersion = FilterField(
        key = "bluetooth_version",
        label = "Bluetooth Version",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("5.0", "5.1", "5.2", "5.3", "5.4", "6.0+", "Other"),
        visibleWhen = FieldDependency("bluetooth_supported", setOf(SUPPORTED_ID))
    )

    val bluetoothFields: List<FilterField> = listOf(bluetoothSupported, bluetoothVersion)

    val chargerIncludedInBox = FilterField(
        key = "charger_included_in_box",
        label = "Charger Included in Box",
        type = FilterType.RadioGroup,
        options = options("Yes", "No")
    )

    val headphoneJack = FilterField(
        key = "headphone_jack_3_5mm",
        label = "3.5mm Headphone Jack",
        type = FilterType.RadioGroup,
        options = options("Yes", "No")
    )
}

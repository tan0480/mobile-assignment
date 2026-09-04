package com.example.gadgetmover.model.filter

/** The Hard Disk Drives (HDD) advanced filter schema — split out from the old combined "Storage" category. */
object HddFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Western Digital", "Seagate", "Toshiba", "HGST", "Synology", "LaCie", "SanDisk Professional", "Unknown"
        )
    )

    val capacity = FilterField(
        key = "capacity",
        label = "Capacity",
        type = FilterType.NumberRangeWithUnitToggle(
            units = listOf(
                FilterType.UnitRange(unit = "GB", min = 250f, max = 2000f, step = 250f, toBaseMultiplier = 1f),
                FilterType.UnitRange(unit = "TB", min = 1f, max = 32f, step = 1f, toBaseMultiplier = 1000f)
            )
        )
    )

    private const val INTERNAL_ID = "internal"
    private const val EXTERNAL_ID = "external"
    val installationType = FilterField(
        key = "installation_type",
        label = "Installation Type",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Internal", "External")
    )

    private val internalFormFactorOptions = options("2.5-inch", "3.5-inch")
    private val externalFormFactorOptions = options("Portable", "Desktop")
    private const val TWO_POINT_FIVE_INCH_ID = "2_5_inch"

    /** Narrows to just the picked [installationType]'s own form-factor list — same dependent-options mechanism as [PhoneFilterSchema.socModel]. */
    val formFactor = FilterField(
        key = "form_factor",
        label = "Form Factor",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = internalFormFactorOptions + externalFormFactorOptions,
        optionsForState = { state ->
            val selected = selectedIdsFor(state, "installation_type")
            when {
                INTERNAL_ID in selected -> internalFormFactorOptions
                EXTERNAL_ID in selected -> externalFormFactorOptions
                else -> internalFormFactorOptions + externalFormFactorOptions
            }
        }
    )

    /** Only meaningful for an internal 2.5-inch drive. */
    val driveHeight = FilterField(
        key = "drive_height",
        label = "2.5-inch Drive Height",
        type = FilterType.NumberRange(min = 5f, max = 15f, step = 0.5f, unit = "mm", unitIsPrefix = false),
        visibleWhen = FieldDependency("form_factor", setOf(TWO_POINT_FIVE_INCH_ID))
    )

    private val internalConnectorOptions = options("SATA", "SAS")
    private val externalConnectorOptions = options("USB Type-A", "USB Type-C", "Thunderbolt")
    private const val SATA_ID = "sata"
    private const val SAS_ID = "sas"

    /** Narrows to just the picked [installationType]'s own connector list — same dependent-options mechanism as [PhoneFilterSchema.socModel]. */
    val connector = FilterField(
        key = "connector",
        label = "Connector",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = internalConnectorOptions + externalConnectorOptions,
        optionsForState = { state ->
            val selected = selectedIdsFor(state, "installation_type")
            when {
                INTERNAL_ID in selected -> internalConnectorOptions
                EXTERNAL_ID in selected -> externalConnectorOptions
                else -> internalConnectorOptions + externalConnectorOptions
            }
        }
    )

    private val sataInterfaceOptions = options("SATA II — 3Gb/s", "SATA III — 6Gb/s", "Not Specified")
    private val sasInterfaceOptions = options("SAS — 6Gb/s", "SAS — 12Gb/s", "SAS — 24Gb/s", "Not Specified")
    private val externalInterfaceOptions = options(
        "USB 3.2 Gen 1 — 5Gbps", "USB 3.2 Gen 2 — 10Gbps", "Thunderbolt 3 — 40Gbps", "Thunderbolt 4 — 40Gbps", "Not Specified"
    )

    /**
     * Narrows to just the picked [connector]'s own speed list — same dependent-options mechanism as
     * [PhoneFilterSchema.socModel]. The interface speed here must not be treated as the HDD's actual
     * transfer speed — see [sustainedTransferSpeed] for that.
     */
    val interfaceVersion = FilterField(
        key = "interface_version",
        label = "Interface Version",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = sataInterfaceOptions + sasInterfaceOptions + externalInterfaceOptions,
        optionsForState = { state ->
            val selected = selectedIdsFor(state, "connector")
            when {
                SATA_ID in selected -> sataInterfaceOptions
                SAS_ID in selected -> sasInterfaceOptions
                selected.isNotEmpty() -> externalInterfaceOptions
                else -> sataInterfaceOptions + sasInterfaceOptions + externalInterfaceOptions
            }
        }
    )

    val rotationalSpeed = FilterField(
        key = "rotational_speed",
        label = "Rotational Speed",
        type = FilterType.NumberRange(min = 5400f, max = 15000f, step = 300f, unit = " rpm", unitIsPrefix = false)
    )

    val cacheSize = FilterField(
        key = "cache_size",
        label = "Cache Size",
        type = FilterType.NumberRange(min = 8f, max = 512f, step = 8f, unit = "MB", unitIsPrefix = false)
    )

    /** HAMR, MAMR, ePMR and similar technologies stay in the detailed specifications — too uncommon, or not directly comparable with the CMR/SMR classification. */
    val recordingTechnology = FilterField(
        key = "recording_technology",
        label = "Recording Technology",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("CMR", "SMR", "Not Specified")
    )

    /** Do not use SATA/SAS interface bandwidth for this — see [interfaceVersion]. */
    val sustainedTransferSpeed = FilterField(
        key = "sustained_transfer_speed",
        label = "Sustained Transfer Speed",
        type = FilterType.NumberRange(min = 50f, max = 500f, step = 10f, unit = " MB/s", unitIsPrefix = false)
    )

    val workloadRating = FilterField(
        key = "workload_rating",
        label = "Workload Rating",
        type = FilterType.NumberRange(min = 50f, max = 600f, step = 10f, unit = " TB/year", unitIsPrefix = false)
    )

    val driveFill = FilterField(
        key = "drive_fill",
        label = "Drive Fill",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Air-Filled", "Helium-Filled", "Not Specified")
    )

    val idleNoise = FilterField(
        key = "idle_noise",
        label = "Idle Noise",
        type = FilterType.NumberRange(min = 15f, max = 40f, step = 1f, unit = " dBA", unitIsPrefix = false)
    )

    val seekNoise = FilterField(
        key = "seek_noise",
        label = "Seek Noise",
        type = FilterType.NumberRange(min = 20f, max = 50f, step = 1f, unit = " dBA", unitIsPrefix = false)
    )

    val peakStartupPower = FilterField(
        key = "peak_startup_power",
        label = "Peak Startup Power",
        type = FilterType.NumberRange(min = 5f, max = 40f, step = 1f, unit = "W", unitIsPrefix = false)
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand, capacity,
            installationType, formFactor, driveHeight, connector, interfaceVersion,
            rotationalSpeed, cacheSize, recordingTechnology, sustainedTransferSpeed, workloadRating,
            driveFill, idleNoise, seekNoise, peakStartupPower
        )
    )
}

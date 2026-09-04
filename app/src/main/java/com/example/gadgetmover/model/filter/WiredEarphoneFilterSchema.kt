package com.example.gadgetmover.model.filter

/** The Wired Earphones (IEMs & earbuds) advanced filter schema. */
object WiredEarphoneFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "Moondrop", "KZ (Knowledge Zenith)", "7Hz", "Tanchjim", "Tin HiFi", "ThieAudio", "Truthear",
            "Etymotic", "Shure", "Sennheiser", "Audio-Technica", "Final Audio", "Campfire Audio", "FiiO",
            "DUNU", "Simgot", "Kiwi Ears", "Letshuoer", "Whizzer", "Yincrow", "BLON", "CCA", "QKZ", "Other"
        )
    )

    val formFactorWearingStyle = FilterField(
        key = "form_factor_wearing_style",
        label = "Form Factor & Wearing Style",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "In-Ear (Over-Ear Cable / IEM Style)", "In-Ear (Straight-Down Cable)", "Flat-Head Earbuds", "Other"
        )
    )

    val driverConfiguration = FilterField(
        key = "driver_configuration",
        label = "Driver Configuration",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Single Dynamic Driver (1 DD)", "Dual / Multi Dynamic Driver (Multi-DD)",
            "Single Balanced Armature (1 BA)", "Multi Balanced Armature (Multi-BA)",
            "Hybrid (Dynamic + Balanced Armature / DD+BA)", "Tribrid (DD + BA + Electrostatic / EST)",
            "Planar Magnetic", "Bone Conduction Hybrid", "Other"
        )
    )

    val driverCountPerSide = FilterField(
        key = "driver_count_per_side",
        label = "Driver Count (Per Side)",
        type = FilterType.NumberRange(min = 1f, max = 16f, step = 1f, unit = "", unitIsPrefix = false)
    )

    val dynamicDriverSize = FilterField(
        key = "dynamic_driver_size",
        label = "Dynamic Driver Size",
        type = FilterType.NumberRange(min = 6f, max = 16f, step = 1f, unit = "mm", unitIsPrefix = false)
    )

    val diaphragmMaterial = FilterField(
        key = "diaphragm_material",
        label = "Diaphragm Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Beryllium / Beryllium-Coated", "DLC (Diamond-Like Carbon)", "LCP (Liquid Crystal Polymer)",
            "CNT (Carbon Nanotube)", "Titanium-Coated", "Bio-Cellulose", "PU + PEEK Composite", "Other"
        )
    )

    val cableMaterialStructure = FilterField(
        key = "cable_material_structure",
        label = "Cable Material & Structure",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "OFC (Oxygen-Free Copper)", "Silver-Plated Copper (SPC)", "OCC / Monocrystalline Copper",
            "Pure Silver", "Gold-Silver-Copper Alloy", "Shielded Coaxial Braided Cable", "Other"
        )
    )

    val cableConnectorType = FilterField(
        key = "cable_connector_type",
        label = "Cable Connector Type (Earphone Side)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "0.78mm 2-Pin", "MMCX", "QDC (Protruding 2-Pin)", "TFZ 2-Pin", "Pentaconn Ear",
            "IPX / T2", "Non-detachable (Fixed Cable)", "Other"
        )
    )

    val audioPlugType = FilterField(
        key = "audio_plug_type",
        label = "Audio Plug Type (Source Side)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "3.5mm Single-Ended (TRS)", "4.4mm Balanced (TRRRS)", "2.5mm Balanced (TRRS)",
            "6.35mm (1/4\") Single-Ended", "USB-C (Built-in DAC / DSP)", "Lightning",
            "Modular / Interchangeable Multi-plug (3.5mm + 4.4mm + 2.5mm)", "Other"
        )
    )

    val eartipMaterial = FilterField(
        key = "eartip_material",
        label = "Eartip Material",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Silicone (Standard)", "Liquid Silicone / Medical Grade", "Memory Foam",
            "Hybrid (Silicone Core + Foam Exterior)", "Double-Flange / Triple-Flange Silicone", "Other"
        )
    )

    val impedance = FilterField(
        key = "impedance",
        label = "Impedance",
        type = FilterType.NumberRange(min = 8f, max = 300f, step = 2f, unit = " Ω", unitIsPrefix = false)
    )

    val sensitivity = FilterField(
        key = "sensitivity",
        label = "Sensitivity",
        type = FilterType.NumberRange(min = 90f, max = 130f, step = 1f, unit = " dB", unitIsPrefix = false)
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

    /** Shares its key with [MiceFilterSchema.weight], so it gets real [com.example.gadgetmover.model.ProductSpecs.weightGrams] matching for free. */
    val weight = FilterField(
        key = "weight",
        label = "Weight",
        type = FilterType.NumberRange(min = 3f, max = 35f, step = 1f, unit = "g", unitIsPrefix = false)
    )

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "In-line Microphone & Remote Control",
            "Tuning Nozzle / Interchangeable Acoustic Filter",
            "Physical Tuning Switches (Dip Switches)",
            "Custom In-Ear Monitor (CIEM Resin Shell)",
            "Detachable Cable",
            "Detachable Gaming Boom Microphone",
            "Other"
        )
    )

    val audioCertifications = FilterField(
        key = "audio_certifications",
        label = "Audio Certifications",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Hi-Res Audio", "THX Certified", "Other")
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            formFactorWearingStyle, driverConfiguration, driverCountPerSide,
            dynamicDriverSize, diaphragmMaterial,
            cableMaterialStructure, cableConnectorType, audioPlugType,
            eartipMaterial,
            impedance, sensitivity, frequencyResponseLow, frequencyResponseHigh,
            weight,
            features,
            audioCertifications
        )
    )
}

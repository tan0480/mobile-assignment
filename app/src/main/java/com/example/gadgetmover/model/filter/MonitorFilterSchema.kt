package com.example.gadgetmover.model.filter

/**
 * The Monitors advanced filter schema. "Video Interfaces & Port Count" nests a port-count choice
 * under each interface type (HDMI 2.1/2.0, DisplayPort 2.1/1.4a) — modeled as one field per
 * interface rather than a single combined field, since each has its own count options. The
 * remaining, non-nested interface entries (DP daisy-chain out, Thunderbolt, USB-C DP Alt Mode,
 * legacy VGA/DVI) live in [otherVideoInterfaces], whose selections gate [usbCPdWattage].
 */
object MonitorFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    private const val CURVED_ID = "curved"
    private const val MINI_LED_IPS_ID = "mini_led_ips"
    private const val MINI_LED_VA_ID = "mini_led_va"
    private const val USB_C_ALT_MODE_ID = "full_featured_usb_c_displayport_alt_mode"
    private const val THUNDERBOLT_ID = "thunderbolt_4_3_with_daisy_chain_in_out"

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "LG", "Samsung", "Dell", "ASUS", "Acer", "BenQ", "ViewSonic", "AOC", "MSI", "Gigabyte",
            "Philips", "Sony", "Alienware", "Corsair", "InnoCN", "KTC", "Other"
        )
    )

    // --- Form Factor & Screen Curvature ---

    val screenCurvature = FilterField(
        key = "screen_curvature",
        label = "Screen Curvature",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Flat", "Curved", "Other")
    )

    val curvatureRating = FilterField(
        key = "curvature_rating",
        label = "Curvature Rating",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("800R", "1000R", "1500R", "1800R", "Other"),
        visibleWhen = FieldDependency("screen_curvature", setOf(CURVED_ID))
    )

    val aspectRatio = FilterField(
        key = "aspect_ratio",
        label = "Aspect Ratio",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "16:9 (Standard Widescreen)", "16:10 (Productivity)", "21:9 (Ultrawide)",
            "32:9 (Super Ultrawide)", "3:2", "Other"
        )
    )

    // --- Display Size & Resolution ---

    val screenSize = FilterField(
        key = "screen_size",
        label = "Screen Size",
        type = FilterType.NumberRange(min = 9f, max = 57.0f, step = 0.5f, unit = "\"", unitIsPrefix = false)
    )

    val resolution = FilterField(
        key = "resolution",
        label = "Resolution",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "1920 × 1080 (FHD)", "1920 × 1200 (WUXGA)", "2560 × 1440 (QHD / 2K)", "2560 × 1600 (WQXGA)",
            "3440 × 1440 (UWQHD)", "3840 × 1600 (UW4K)", "3840 × 2160 (UHD / 4K)", "5120 × 1440 (Dual QHD)",
            "5120 × 2160 (5K2K / WUHD)", "5120 × 2880 (5K)", "6016 × 3384 (6K)", "7680 × 2160 (Dual 4K)", "Other"
        )
    )

    // --- Panel Technology & Backlight ---

    val panelType = FilterField(
        key = "panel_type",
        label = "Panel Type",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "QD-OLED", "WOLED", "Mini-LED IPS", "Mini-LED VA", "Fast IPS / Nano IPS",
            "Standard IPS", "Fast VA / HVA", "Standard VA", "TN", "Other"
        )
    )

    val miniLedLocalDimmingZones = FilterField(
        key = "mini_led_local_dimming_zones",
        label = "Mini-LED Local Dimming Zones",
        type = FilterType.NumberRange(min = 384f, max = 2304f, step = 96f, unit = " Zones", unitIsPrefix = false),
        visibleWhen = FieldDependency("panel_type", setOf(MINI_LED_IPS_ID, MINI_LED_VA_ID))
    )

    val pixelResponseTimeGtg = FilterField(
        key = "pixel_response_time_gtg",
        label = "Pixel Response Time (GtG)",
        type = FilterType.NumberRange(min = 0f, max = 10f, step = 0.5f, unit = " ms", unitIsPrefix = false)
    )

    val pixelResponseTimeMprt = FilterField(
        key = "pixel_response_time_mprt",
        label = "Pixel Response Time (MPRT)",
        type = FilterType.NumberRange(min = 0f, max = 10f, step = 0.5f, unit = " ms", unitIsPrefix = false)
    )

    // --- Refresh Rate & Adaptive Sync ---

    val refreshRate = FilterField(
        key = "refresh_rate",
        label = "Refresh Rate",
        type = FilterType.NumberRange(min = 60f, max = 540f, step = 10f, unit = " Hz", unitIsPrefix = false)
    )

    val vrrSyncTechnology = FilterField(
        key = "vrr_sync_technology",
        label = "Variable Refresh Rate (VRR) / Sync Technology",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "NVIDIA G-Sync Ultimate", "NVIDIA G-Sync Compatible", "AMD FreeSync Premium Pro",
            "AMD FreeSync Premium", "VESA Adaptive-Sync", "Other"
        )
    )

    // --- Color Performance & HDR ---

    val colorDepth = FilterField(
        key = "color_depth",
        label = "Color Depth",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("8-bit", "8-bit + FRC (10-bit Simulated)", "True 10-bit", "12-bit", "Other")
    )

    val srgbCoverage = FilterField(
        key = "srgb_coverage",
        label = "Color Gamut — sRGB Coverage",
        type = FilterType.NumberRange(min = 90f, max = 100f, step = 1f, unit = "%", unitIsPrefix = false)
    )

    val dciP3Coverage = FilterField(
        key = "dci_p3_coverage",
        label = "Color Gamut — DCI-P3 Coverage",
        type = FilterType.NumberRange(min = 90f, max = 100f, step = 1f, unit = "%", unitIsPrefix = false)
    )

    val adobeRgbCoverage = FilterField(
        key = "adobe_rgb_coverage",
        label = "Color Gamut — Adobe RGB Coverage",
        type = FilterType.NumberRange(min = 90f, max = 100f, step = 1f, unit = "%", unitIsPrefix = false)
    )

    val colorAccuracy = FilterField(
        key = "color_accuracy",
        label = "Color Accuracy (Delta E / ΔE)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("Factory Calibrated (ΔE < 2)", "Factory Calibrated (ΔE < 1)", "Uncalibrated", "Other")
    )

    val hdrStandard = FilterField(
        key = "hdr_standard",
        label = "HDR Standard & Certification",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "VESA DisplayHDR 400", "VESA DisplayHDR 600", "VESA DisplayHDR 1000", "VESA DisplayHDR 1400",
            "VESA DisplayHDR True Black 400 (OLED)", "VESA DisplayHDR True Black 500 (OLED)",
            "Dolby Vision", "Other"
        )
    )

    val peakBrightness = FilterField(
        key = "peak_brightness",
        label = "Peak Brightness",
        type = FilterType.NumberRange(min = 250f, max = 2000f, step = 50f, unit = " nits", unitIsPrefix = false)
    )

    // --- Connectivity & I/O Ports ---

    val hdmi21Ports = FilterField(
        key = "hdmi_2_1_ports",
        label = "HDMI 2.1 Ports (Full Bandwidth / eARC)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("1 × Port", "2 × Ports", "3 × Ports+")
    )

    val hdmi20Ports = FilterField(
        key = "hdmi_2_0_ports",
        label = "HDMI 2.0 Ports",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("1 × Port", "2 × Ports", "3 × Ports+")
    )

    val displayPort21Ports = FilterField(
        key = "displayport_2_1_ports",
        label = "DisplayPort 2.1 Ports (UHBR10 / UHBR20)",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("1 × Port", "2 × Ports")
    )

    val displayPort14aPorts = FilterField(
        key = "displayport_1_4a_ports",
        label = "DisplayPort 1.4a Ports",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("1 × Port", "2 × Ports", "3 × Ports+")
    )

    val otherVideoInterfaces = FilterField(
        key = "other_video_interfaces",
        label = "Other Video Interfaces",
        type = FilterType.CheckboxList,
        options = options(
            "DisplayPort Out (Daisy Chain MST Support)", "Thunderbolt 4 / 3 (With Daisy Chain In/Out)",
            "Full-Featured USB-C (DisplayPort Alt Mode)", "VGA / DVI (Legacy Ports)", "Other"
        )
    )

    val totalVideoInputPorts = FilterField(
        key = "total_video_input_ports",
        label = "Total Video Input Ports Count",
        type = FilterType.NumberRange(min = 1f, max = 6f, step = 1f, unit = " Ports", unitIsPrefix = false)
    )

    /** Only meaningful once the monitor actually has a USB-C Alt Mode or Thunderbolt input to carry power delivery. */
    val usbCPdWattage = FilterField(
        key = "usb_c_pd_wattage",
        label = "USB-C Power Delivery (PD) Wattage",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("15W", "65W", "90W / 96W", "100W", "140W", "Other"),
        visibleWhen = FieldDependency("other_video_interfaces", setOf(USB_C_ALT_MODE_ID, THUNDERBOLT_ID))
    )

    val usbADownstreamPorts = FilterField(
        key = "usb_a_downstream_ports",
        label = "USB-A Downstream Ports",
        type = FilterType.NumberRange(min = 1f, max = 5f, step = 1f, unit = " Ports", unitIsPrefix = false)
    )

    val usbCDownstreamPorts = FilterField(
        key = "usb_c_downstream_ports",
        label = "USB-C Data Downstream Ports",
        type = FilterType.NumberRange(min = 1f, max = 3f, step = 1f, unit = " Ports", unitIsPrefix = false)
    )

    val otherIoPorts = FilterField(
        key = "other_io_ports",
        label = "Other I/O Ports",
        type = FilterType.CheckboxList,
        options = options(
            "USB-B / USB-C Upstream (PC Data Link)", "3.5mm Audio Out (Headphone Jack)", "3.5mm Audio In (AUX)",
            "Optical Audio Out (SPDIF)", "RJ-45 Ethernet Port (LAN Hub / Docking)", "Other"
        )
    )

    // --- Ergonomics & Stand Adjustments ---

    val standAdjustability = FilterField(
        key = "stand_adjustability",
        label = "Stand Adjustability",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Height Adjustable", "Tilt Adjustment (-5° to +20°)", "Swivel Adjustment (Left / Right)",
            "Pivot Adjustment (90° Vertical Rotation)", "Other"
        )
    )

    val vesaMountSupport = FilterField(
        key = "vesa_mount_support",
        label = "VESA Wall Mount Support",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("VESA 75 × 75 mm", "VESA 100 × 100 mm", "Non-standard / Bracket Required", "Other")
    )

    // --- Audio & Webcam ---

    val builtInSpeakers = FilterField(
        key = "built_in_speakers",
        label = "Built-in Speakers",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "No Built-in Speakers", "2 × 2W / 3W (Standard)", "2 × 5W+ (High Power)",
            "Integrated Subwoofer", "Other"
        )
    )

    val builtInWebcam = FilterField(
        key = "built_in_webcam",
        label = "Built-in Webcam",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("1080p FHD Pop-up Webcam", "4K UHD Webcam", "Windows Hello IR Sensor", "No Webcam", "Other")
    )

    // --- Eye Care & Features ---

    val eyeCare = FilterField(
        key = "eye_care",
        label = "Eye Care",
        type = FilterType.CheckboxList,
        options = options(
            "Hardware Low Blue Light (TÜV Rheinland Certified)", "Flicker-Free DC Dimming",
            "Anti-Glare / Matte Coating", "Glossy Coating", "Low-Reflection Glass (OLED Glare-Free)",
            "Ambient Light Sensor (Auto-Brightness)", "Other"
        )
    )

    val advancedFeatures = FilterField(
        key = "advanced_features",
        label = "Advanced Features",
        type = FilterType.CheckboxList,
        options = options(
            "Built-in KVM Switch (One Keyboard/Mouse for 2 PCs)", "Picture-in-Picture (PiP) / Picture-by-Picture (PbP)",
            "OLED Anti-Burn-In Protection Suite", "Crosshair / Black Equalizer / Gaming OSD",
            "Uniformity Compensation (For Color Grading)", "RGB Ambient Backlight (Ambiglow / LightSync)", "Other"
        )
    )

    val schema = CategoryFilterSchema(
        sections = listOf(
            brand,
            screenCurvature, curvatureRating, aspectRatio,
            screenSize, resolution,
            panelType, miniLedLocalDimmingZones, pixelResponseTimeGtg, pixelResponseTimeMprt,
            refreshRate, vrrSyncTechnology,
            colorDepth, srgbCoverage, dciP3Coverage, adobeRgbCoverage, colorAccuracy, hdrStandard, peakBrightness,
            hdmi21Ports, hdmi20Ports, displayPort21Ports, displayPort14aPorts, otherVideoInterfaces,
            totalVideoInputPorts, usbCPdWattage,
            usbADownstreamPorts, usbCDownstreamPorts, otherIoPorts,
            standAdjustability, vesaMountSupport,
            builtInSpeakers, builtInWebcam,
            eyeCare, advancedFeatures
        )
    )
}

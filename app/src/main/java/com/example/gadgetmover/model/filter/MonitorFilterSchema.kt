package com.example.gadgetmover.model.filter

/**
 * The Monitors advanced filter schema. [videoPorts] replaces what used to be one fixed field per
 * port generation (HDMI 2.1/2.0, DisplayPort 2.1/1.4a) plus a flat "Other Video Interfaces"
 * checklist with a single repeatable "Add Port" builder against [VideoPortCatalog] — pick a port
 * type first, then (listing mode only) how many; the system derives the total from the list
 * instead of a separately-entered count. [usbCPdWattage] is gated on whichever port types were
 * actually added, not a standalone toggle.
 */
object MonitorFilterSchema {

    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    private const val CURVED_ID = "curved"
    private const val MINI_LED_IPS_ID = "mini_led_ips"
    private const val MINI_LED_VA_ID = "mini_led_va"
    private val USB_C_PD_CAPABLE_PORT_IDS = setOf("usb_c_dp_alt_mode", "thunderbolt_3", "thunderbolt_4", "thunderbolt_5")

    val brand = FilterField(
        key = "brand",
        label = "Brand",
        type = FilterType.SearchablePopupSelect(isMultiSelect = false, allowCustomInput = true),
        options = options(
            "Acer", "AOC", "Apple", "ASRock", "ASUS", "BenQ", "Cooler Master", "Corsair", "Dell", "Dough",
            "Eizo", "Gigabyte", "HKC", "HP", "Huawei", "Innocn", "Iiyama", "KTC", "Lenovo", "LG", "MSI",
            "NEC", "NZXT", "Philips", "Pixio", "Prism+", "Samsung", "Sceptre", "Sharp", "Sony", "TCL",
            "Titan Army", "ViewSonic", "Xiaomi", "Unknown"
        )
    )

    // --- Form Factor & Screen Curvature ---

    val screenCurvature = FilterField(
        key = "screen_curvature",
        label = "Screen Curvature",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Flat", "Curved", "Other")
    )

    val curvatureRating = FilterField(
        key = "curvature_rating",
        label = "Curvature Rating",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("800R", "1000R", "1500R", "1800R", "Other"),
        visibleWhen = FieldDependency("screen_curvature", setOf(CURVED_ID))
    )

    val aspectRatio = FilterField(
        key = "aspect_ratio",
        label = "Aspect Ratio",
        type = FilterType.ChipGroup(isMultiSelect = false),
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
        type = FilterType.ChipGroup(isMultiSelect = false),
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
        type = FilterType.ChipGroup(isMultiSelect = false),
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
            "None / Fixed Refresh Rate", "AMD FreeSync", "NVIDIA G-SYNC", "HDMI Forum VRR",
            "Intel Adaptive Sync", "Unknown / Not Specified"
        )
    )

    // --- Color Performance & HDR ---

    val colorDepth = FilterField(
        key = "color_depth",
        label = "Color Depth",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "6-bit", "6-bit + FRC", "8-bit", "8-bit + FRC (10-bit Simulated)", "True 10-bit", "12-bit",
            "Unknown / Not Specified", "Other"
        )
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
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options(
            "Factory Calibrated — ΔE < 1", "Factory Calibrated — ΔE < 2", "Factory Calibrated — ΔE < 3",
            "Factory Calibrated — ΔE Not Specified", "Not Factory Calibrated", "Unknown / Not Specified"
        )
    )

    val hdrStandard = FilterField(
        key = "hdr_standard",
        label = "HDR Standard & Certification",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "VESA DisplayHDR 400", "VESA DisplayHDR 500", "VESA DisplayHDR 600", "VESA DisplayHDR 1000",
            "VESA DisplayHDR 1400", "VESA DisplayHDR True Black 400", "VESA DisplayHDR True Black 500",
            "VESA DisplayHDR True Black 600", "VESA DisplayHDR True Black 1000", "VESA DisplayHDR True Black 1400",
            "HDR Supported — Not Certified", "No HDR", "Unknown"
        )
    )

    val peakBrightness = FilterField(
        key = "peak_brightness",
        label = "Peak Brightness",
        type = FilterType.NumberRange(min = 250f, max = 2000f, step = 50f, unit = " nits", unitIsPrefix = false)
    )

    // --- Connectivity & I/O Ports ---

    /** Repeatable "Add Port" builder — a monitor's exact port mix (e.g. 2× HDMI 2.1 + 1× DisplayPort 1.4a) is described one entry at a time instead of a fixed field per port generation; see [VideoPortRequirement]. */
    val videoPorts = FilterField(
        key = "video_ports",
        label = "Video Ports",
        type = FilterType.VideoPortBuilder
    )

    /** Only meaningful once the monitor actually has a USB-C Alt Mode or Thunderbolt port added to carry power delivery. */
    val usbCPdWattage = FilterField(
        key = "usb_c_pd_wattage",
        label = "USB-C Power Delivery (PD) Wattage",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options("15W", "65W", "90W / 96W", "100W", "140W", "Other"),
        visibleWhen = FieldDependency("video_ports", USB_C_PD_CAPABLE_PORT_IDS)
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
            "Optical Audio Out (SPDIF)", "RJ-45 Ethernet Port (LAN Hub / Docking)",
            "Thunderbolt Upstream", "3.5 mm Headset Combo Jack", "SD Card Reader", "microSD Card Reader",
            "RS-232 / Serial Control", "Other"
        )
    )

    // --- Ergonomics & Stand Adjustments ---

    val standAdjustability = FilterField(
        key = "stand_adjustability",
        label = "Stand Adjustability",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = options(
            "Fixed Stand / No Adjustment", "Height Adjustable", "Tilt Adjustable", "Swivel Adjustable",
            "Pivot 90° Clockwise", "Pivot 90° Counterclockwise", "Pivot Both Directions",
            "Landscape/Portrait Auto-Rotation", "Adjustable Monitor Arm Included",
            "Foldable Stand / Kickstand", "Detachable Stand", "Other"
        )
    )

    /** Whether/how the monitor mounts on VESA hardware — distinct from [vesaMountSupport]'s specific hole-pattern size, which only matters once a monitor is VESA-mountable at all. */
    val mounting = FilterField(
        key = "mounting",
        label = "Mounting",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = options("Proprietary Mount", "VESA Adapter Required", "Not VESA Mountable", "Other")
    )

    val vesaMountSupport = FilterField(
        key = "vesa_mount_support",
        label = "VESA Wall Mount Support",
        type = FilterType.ChipGroup(isMultiSelect = false),
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
        type = FilterType.ChipGroup(isMultiSelect = false),
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

    val features = FilterField(
        key = "features",
        label = "Features",
        type = FilterType.CheckboxList,
        options = options(
            "Built-in KVM Switch (One Keyboard/Mouse for 2 PCs)", "Picture-in-Picture (PiP) / Picture-by-Picture (PbP)",
            "OLED Anti-Burn-In Protection Suite", "Crosshair / Black Equalizer / Gaming OSD",
            "Uniformity Compensation (For Color Grading)", "RGB Ambient Backlight (Ambiglow / LightSync)"
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
            videoPorts, usbCPdWattage,
            usbADownstreamPorts, usbCDownstreamPorts, otherIoPorts,
            standAdjustability, mounting, vesaMountSupport,
            builtInSpeakers, builtInWebcam,
            eyeCare, features
        )
    )
}

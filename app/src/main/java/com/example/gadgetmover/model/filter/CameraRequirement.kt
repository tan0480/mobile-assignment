package com.example.gadgetmover.model.filter

import kotlinx.serialization.Serializable

/** Which physical role a [CameraRequirement] describes — the same roles [RearCameraModuleFields] used to gate a fixed checkbox list on, now a per-requirement choice inside a repeatable list instead. */
@Serializable
enum class CameraRole(val label: String) {
    MAIN("Main Camera"),
    ULTRA_WIDE("Ultra-wide"),
    TELEPHOTO("Telephoto"),
    PERISCOPE_TELEPHOTO("Periscope Telephoto"),
    MACRO("Macro"),
    DEPTH("Depth"),
    MONOCHROME("Monochrome"),
    OTHER("Other")
}

/**
 * How a numeric/orderable camera spec (Resolution, Sensor Size, Aperture, Optical Zoom) is being
 * compared — a precise value, a floor, or a closed range. Real phones use non-integer values (a
 * 4.3x zoom, an f/1.68 aperture), so none of these can be a fixed checkbox list.
 */
@Serializable
enum class NumericRequirementMode(val label: String) {
    EXACT("Exact value"),
    MINIMUM("Minimum value"),
    RANGE("Range")
}

/** Which of [CameraRequirement]'s optional fields apply for a given [CameraRole] — drives which controls a requirement card shows. */
enum class CameraRequirementField {
    RESOLUTION, SENSOR_SIZE, APERTURE, FOV, OPTICAL_ZOOM, OIS, EIS
}

fun CameraRole.visibleFields(): Set<CameraRequirementField> = when (this) {
    CameraRole.MAIN -> setOf(
        CameraRequirementField.RESOLUTION, CameraRequirementField.SENSOR_SIZE, CameraRequirementField.APERTURE,
        CameraRequirementField.OIS, CameraRequirementField.EIS
    )
    CameraRole.ULTRA_WIDE -> setOf(
        CameraRequirementField.RESOLUTION, CameraRequirementField.SENSOR_SIZE, CameraRequirementField.FOV,
        CameraRequirementField.OIS, CameraRequirementField.EIS
    )
    CameraRole.TELEPHOTO -> setOf(
        CameraRequirementField.RESOLUTION, CameraRequirementField.SENSOR_SIZE,
        CameraRequirementField.OPTICAL_ZOOM, CameraRequirementField.APERTURE,
        CameraRequirementField.OIS, CameraRequirementField.EIS
    )
    CameraRole.PERISCOPE_TELEPHOTO -> setOf(
        CameraRequirementField.RESOLUTION, CameraRequirementField.SENSOR_SIZE,
        CameraRequirementField.OPTICAL_ZOOM,
        CameraRequirementField.OIS, CameraRequirementField.EIS
    )
    CameraRole.MACRO -> setOf(CameraRequirementField.RESOLUTION)
    CameraRole.DEPTH, CameraRole.MONOCHROME -> setOf(CameraRequirementField.RESOLUTION)
    CameraRole.OTHER -> setOf(
        CameraRequirementField.RESOLUTION, CameraRequirementField.SENSOR_SIZE, CameraRequirementField.APERTURE,
        CameraRequirementField.OIS, CameraRequirementField.EIS
    )
}

/** Shared across every role rather than duplicated per-role catalogues like [RearCameraModuleFields] used to keep — one option list, picked as exact/minimum/range. */
object CameraSensorSizeOptions {
    val all: List<FilterOption> = listOf(
        "1/1.0\"", "1/1.14\"", "1/1.3\"", "1/1.4\"", "1/1.5\"", "1/1.56\"", "1/1.7\"",
        "1/2.0\"", "1/2.5\"", "1/2.51\"", "1/2.76\"", "1/3.0\"", "Other"
    ).map { FilterOption(id = it.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_'), label = it) }
}

/** Hard cap enforced by the "+ Add Camera" button — a phone's camera system realistically never needs more requirement slots than this. */
const val MAX_CAMERA_REQUIREMENTS = 10

/**
 * One independent "the camera system must include a camera like this" slot inside a
 * [FilterFieldValue.CameraRequirements] list. Every requirement carries its own [role] and values —
 * a search for "50MP+ main AND 40MP+ ultra-wide AND 5x+ periscope" is three of these, matched
 * against a phone's actual cameras without regard to order (see `CameraRequirementMatching`).
 *
 * Resolution, Sensor Size, Aperture and Optical Zoom each carry their own [NumericRequirementMode]
 * (exact, minimum, or range) instead of being fixed to "minimum only" — the Exact, Min, and Max
 * fields for a given spec are only meaningful when that spec's mode selects them (e.g.
 * `resolutionMaxMp` is only read when `resolutionMode == RANGE`).
 */
@Serializable
data class CameraRequirement(
    val id: String,
    val role: CameraRole = CameraRole.MAIN,

    val resolutionMode: NumericRequirementMode = NumericRequirementMode.MINIMUM,
    val resolutionExactMp: String = "",
    val resolutionMinMp: String = "",
    val resolutionMaxMp: String = "",

    val sensorSizeMode: NumericRequirementMode = NumericRequirementMode.MINIMUM,
    val sensorSizeExactId: String? = null,
    val sensorSizeMinId: String? = null,
    val sensorSizeMaxId: String? = null,

    val apertureMode: NumericRequirementMode = NumericRequirementMode.MINIMUM,
    val apertureExactF: String = "",
    val apertureMinF: String = "",
    val apertureMaxF: String = "",

    val fovMinDegrees: String = "",

    val zoomMode: NumericRequirementMode = NumericRequirementMode.MINIMUM,
    val zoomExact: String = "",
    val zoomMin: String = "",
    val zoomMax: String = "",

    val oisRequired: Boolean = false,
    val eisRequired: Boolean = false
)

/** Short one-line description for a collapsed requirement card, e.g. "Main Camera • ≥50 MP • ≥1/1.3" • OIS". */
fun CameraRequirement.summaryText(): String {
    val parts = mutableListOf(role.label)
    numericSummary(resolutionMode, resolutionExactMp, resolutionMinMp, resolutionMaxMp, " MP")?.let { parts += it }
    sensorSizeSummary()?.let { parts += it }
    numericSummary(apertureMode, apertureExactF, apertureMinF, apertureMaxF, "", prefix = "f/")?.let { parts += it }
    fovMinDegrees.toFloatOrNull()?.let { parts += "≥${it.toCleanString()}° FOV" }
    numericSummary(zoomMode, zoomExact, zoomMin, zoomMax, "×")?.let { parts += it }
    if (oisRequired) parts += "OIS"
    if (eisRequired) parts += "EIS"
    return parts.joinToString(" • ")
}

private fun numericSummary(mode: NumericRequirementMode, exact: String, min: String, max: String, unit: String, prefix: String = ""): String? {
    fun fmt(v: Float) = "$prefix${v.toCleanString()}$unit"
    return when (mode) {
        NumericRequirementMode.EXACT -> exact.toFloatOrNull()?.let { fmt(it) }
        NumericRequirementMode.MINIMUM -> min.toFloatOrNull()?.let { "≥${fmt(it)}" }
        NumericRequirementMode.RANGE -> {
            val mn = min.toFloatOrNull(); val mx = max.toFloatOrNull()
            if (mn != null && mx != null) "${fmt(mn)}–${fmt(mx)}" else null
        }
    }
}

private fun CameraRequirement.sensorSizeSummary(): String? {
    fun label(id: String?) = id?.let { i -> CameraSensorSizeOptions.all.find { it.id == i }?.label }
    return when (sensorSizeMode) {
        NumericRequirementMode.EXACT -> label(sensorSizeExactId)
        NumericRequirementMode.MINIMUM -> label(sensorSizeMinId)?.let { "≥$it" }
        NumericRequirementMode.RANGE -> {
            val mn = label(sensorSizeMinId); val mx = label(sensorSizeMaxId)
            if (mn != null && mx != null) "$mn–$mx" else null
        }
    }
}

private fun Float.toCleanString(): String =
    if (this == this.toInt().toFloat()) this.toInt().toString() else "%.1f".format(this)

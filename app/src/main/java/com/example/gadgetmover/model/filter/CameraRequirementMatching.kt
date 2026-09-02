package com.example.gadgetmover.model.filter

/**
 * One of a phone's actual physical rear cameras, in the shape a [CameraRequirement] needs to be
 * checked against. A product's stored camera system is itself a `List<CameraRequirement>` (the
 * seller fills it in with the same [FilterType.CameraSystemBuilder] widget a buyer's search
 * filter uses), so [toActualCameraModule] bridges the two rather than anything constructing this
 * directly — see [satisfiesAllCameraRequirements] below and [CategoryFilterMatching].
 */
data class ActualCameraModule(
    val role: CameraRole,
    val resolutionMp: Float? = null,
    /** Physically comparable sensor size — larger value means a bigger sensor (the inverse of the "1/x"" denominator convention this app's UI uses). */
    val sensorSizeValue: Float? = null,
    val apertureFNumber: Float? = null,
    val fovDegrees: Float? = null,
    val opticalZoomMin: Float? = null,
    val opticalZoomMax: Float? = null,
    val oisSupported: Boolean = false,
    val eisSupported: Boolean = false
)

/** Converts a "1/1.3"" sensor-size option id into a comparable size value (larger = physically bigger sensor). */
private fun sensorSizeIdToValue(id: String): Float? {
    val label = CameraSensorSizeOptions.all.find { it.id == id }?.label ?: return null
    val denominator = Regex("""1/([0-9.]+)""").find(label)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
    return 1f / denominator
}

/** True if [actual] satisfies a [mode]-shaped numeric requirement against a comparable [value]. */
private fun numericSatisfied(mode: NumericRequirementMode, exact: String, min: String, max: String, actual: Float?): Boolean {
    val value = actual ?: return false
    return when (mode) {
        NumericRequirementMode.EXACT -> exact.toFloatOrNull()?.let { it == value } ?: true
        NumericRequirementMode.MINIMUM -> min.toFloatOrNull()?.let { value >= it } ?: true
        NumericRequirementMode.RANGE -> {
            val mn = min.toFloatOrNull(); val mx = max.toFloatOrNull()
            if (mn != null && mx != null) value in mn..mx else true
        }
    }
}

/** True if [camera] alone can satisfy every constraint [this] requirement sets. */
fun CameraRequirement.isSatisfiedBy(camera: ActualCameraModule): Boolean {
    if (role != camera.role) return false

    if (!numericSatisfied(resolutionMode, resolutionExactMp, resolutionMinMp, resolutionMaxMp, camera.resolutionMp)) return false
    if (!numericSatisfied(apertureMode, apertureExactF, apertureMinF, apertureMaxF, camera.apertureFNumber)) return false

    val requiredSensorValue = when (sensorSizeMode) {
        NumericRequirementMode.EXACT -> sensorSizeExactId?.let(::sensorSizeIdToValue)
        NumericRequirementMode.MINIMUM -> sensorSizeMinId?.let(::sensorSizeIdToValue)
        NumericRequirementMode.RANGE -> null
    }
    if (sensorSizeMode != NumericRequirementMode.RANGE && requiredSensorValue != null) {
        val camValue = camera.sensorSizeValue ?: return false
        val ok = if (sensorSizeMode == NumericRequirementMode.EXACT) camValue == requiredSensorValue else camValue >= requiredSensorValue
        if (!ok) return false
    } else if (sensorSizeMode == NumericRequirementMode.RANGE) {
        val mn = sensorSizeMinId?.let(::sensorSizeIdToValue)
        val mx = sensorSizeMaxId?.let(::sensorSizeIdToValue)
        if (mn != null && mx != null) {
            val camValue = camera.sensorSizeValue ?: return false
            if (camValue < mn || camValue > mx) return false
        }
    }

    fovMinDegrees.toFloatOrNull()?.let { min -> if ((camera.fovDegrees ?: return false) < min) return false }

    when (zoomMode) {
        NumericRequirementMode.EXACT -> zoomExact.toFloatOrNull()?.let { exact ->
            val camMin = camera.opticalZoomMin ?: return false
            val camMax = camera.opticalZoomMax ?: camMin
            if (exact < camMin || exact > camMax) return false
        }
        NumericRequirementMode.MINIMUM -> zoomMin.toFloatOrNull()?.let { min ->
            val camMax = camera.opticalZoomMax ?: camera.opticalZoomMin ?: return false
            if (camMax < min) return false
        }
        NumericRequirementMode.RANGE -> {
            val reqMin = zoomMin.toFloatOrNull()
            val reqMax = zoomMax.toFloatOrNull()
            if (reqMin != null && reqMax != null) {
                val camMin = camera.opticalZoomMin ?: return false
                val camMax = camera.opticalZoomMax ?: camMin
                if (camMax < reqMin || camMin > reqMax) return false
            }
        }
    }

    if (oisRequired && !camera.oisSupported) return false
    if (eisRequired && !camera.eisSupported) return false

    return true
}

/**
 * True only if every requirement in [this] list can be matched to a *different* camera in
 * [cameras] — order-independent (a phone's Camera A/B/C can satisfy the user's Camera 1/2/3 in any
 * order), but one physical camera can never be double-booked to satisfy two requirements at once.
 * Implemented as maximum bipartite matching (Kuhn's algorithm): requirement counts are always
 * small (at most [MAX_CAMERA_REQUIREMENTS]), so a plain augmenting-path search is more than fast
 * enough — no need for anything like Hopcroft-Karp here.
 */
fun List<CameraRequirement>.satisfiesAllCameraRequirements(cameras: List<ActualCameraModule>): Boolean {
    if (isEmpty()) return true
    val matchedCameraForRequirement = IntArray(size) { -1 }

    fun tryAssign(reqIndex: Int, visited: BooleanArray): Boolean {
        for (camIndex in cameras.indices) {
            if (visited[camIndex]) continue
            if (!this[reqIndex].isSatisfiedBy(cameras[camIndex])) continue
            visited[camIndex] = true
            val currentOwner = matchedCameraForRequirement.indexOf(camIndex)
            if (currentOwner == -1 || tryAssign(currentOwner, visited)) {
                matchedCameraForRequirement[reqIndex] = camIndex
                return true
            }
        }
        return false
    }

    var matchedCount = 0
    for (reqIndex in indices) {
        if (tryAssign(reqIndex, BooleanArray(cameras.size))) matchedCount++
    }
    return matchedCount == size
}

/** Resolves a [mode]-shaped numeric entry down to one concrete value — [NumericRequirementMode.EXACT]/[NumericRequirementMode.MINIMUM] use their one entered number as-is, [NumericRequirementMode.RANGE] takes the midpoint (or whichever bound was actually entered, if only one was). */
private fun resolvedNumericValue(mode: NumericRequirementMode, exact: String, min: String, max: String): Float? = when (mode) {
    NumericRequirementMode.EXACT -> exact.toFloatOrNull()
    NumericRequirementMode.MINIMUM -> min.toFloatOrNull()
    NumericRequirementMode.RANGE -> {
        val mn = min.toFloatOrNull()
        val mx = max.toFloatOrNull()
        if (mn != null && mx != null) (mn + mx) / 2f else mn ?: mx
    }
}

private fun resolvedSensorSizeId(mode: NumericRequirementMode, exactId: String?, minId: String?, maxId: String?): String? = when (mode) {
    NumericRequirementMode.EXACT -> exactId
    NumericRequirementMode.MINIMUM -> minId
    NumericRequirementMode.RANGE -> minId ?: maxId
}

/**
 * Now that the listing wizard fills in a product's actual camera system using the same
 * [FilterType.CameraSystemBuilder] widget a buyer's search filter uses (see
 * `ListingWizardScreen`/`CategoryFilterMatching`), a product's stored data is also a
 * `List<CameraRequirement>` rather than an [ActualCameraModule] list — this bridges the two by
 * collapsing whichever [NumericRequirementMode] the seller used down to the one concrete value it
 * describes, so [satisfiesAllCameraRequirements] can compare a buyer's requirement against it.
 */
fun CameraRequirement.toActualCameraModule(): ActualCameraModule = ActualCameraModule(
    role = role,
    resolutionMp = resolvedNumericValue(resolutionMode, resolutionExactMp, resolutionMinMp, resolutionMaxMp),
    sensorSizeValue = resolvedSensorSizeId(sensorSizeMode, sensorSizeExactId, sensorSizeMinId, sensorSizeMaxId)?.let(::sensorSizeIdToValue),
    apertureFNumber = resolvedNumericValue(apertureMode, apertureExactF, apertureMinF, apertureMaxF),
    fovDegrees = fovMinDegrees.toFloatOrNull(),
    // RANGE mode describes a real min-max continuous zoom (e.g. "5x-10x telephoto"); the other
    // modes describe one concrete value, so min and max collapse to the same point.
    opticalZoomMin = if (zoomMode == NumericRequirementMode.RANGE) zoomMin.toFloatOrNull() else resolvedNumericValue(zoomMode, zoomExact, zoomMin, zoomMax),
    opticalZoomMax = if (zoomMode == NumericRequirementMode.RANGE) zoomMax.toFloatOrNull() else resolvedNumericValue(zoomMode, zoomExact, zoomMin, zoomMax),
    oisSupported = oisRequired,
    eisSupported = eisRequired
)

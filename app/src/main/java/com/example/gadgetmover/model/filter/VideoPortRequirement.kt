package com.example.gadgetmover.model.filter

import kotlinx.serialization.Serializable

/** Hard cap enforced by the "+ Add Port" button — a monitor realistically never needs more port entries than this. */
const val MAX_VIDEO_PORT_REQUIREMENTS = 12

/**
 * One independent "this monitor has N of this port" slot inside a
 * [FilterFieldValue.VideoPortRequirements] list — replaces the old fixed one-field-per-generation
 * design (separate HDMI 2.1/2.0 and DisplayPort 2.1/1.4a count fields, plus a flat "Other Video
 * Interfaces" checklist) with a single repeatable builder, the same pattern [SwitchRequirement]
 * uses but against one flat catalogue ([VideoPortCatalog]) instead of a brand-then-model narrowing.
 * [quantity] is always an exact count in listing mode; buyer search mode leaves it blank and only
 * matches on [portTypeId] (see `CategoryFilterMatching`).
 */
@Serializable
data class VideoPortRequirement(
    val id: String,
    val portTypeId: String? = null,
    val quantity: String = ""
)

/** Short one-line description for a collapsed requirement card, e.g. "DisplayPort 1.4a × 2". */
fun VideoPortRequirement.summaryText(): String {
    val typeLabel = portTypeId?.let { id ->
        if (FilterFieldValue.isCustomId(id)) FilterFieldValue.customLabel(id)
        else VideoPortCatalog.portTypes.find { it.id == id }?.label
    } ?: "Select Port Type"
    return if (quantity.isNotBlank()) "$typeLabel × $quantity" else typeLabel
}

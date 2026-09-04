package com.example.gadgetmover.model.filter

import kotlinx.serialization.Serializable

/** Hard cap enforced by the "+ Add Switch" button — a keyboard listing realistically never needs more switch entries than this. */
const val MAX_SWITCH_REQUIREMENTS = 10

/**
 * One independent "this keyboard includes N of this switch" slot inside a
 * [FilterFieldValue.SwitchRequirements] list — the same repeatable-card pattern
 * [CameraRequirement] uses, but simpler: [brandId]/[modelId] are picked from [SwitchCatalog]
 * (brand first, model list narrowed to that brand, mirroring [CameraRole] then its spec fields),
 * and [quantity] is always an exact count rather than a min/range — a real keyboard is built with
 * a concrete number of a given switch, not a range of one. Listing mode fills in [quantity];
 * buyer search mode leaves it blank and only matches on [brandId]/[modelId] (see
 * `CategoryFilterMatching`).
 */
@Serializable
data class SwitchRequirement(
    val id: String,
    val brandId: String? = null,
    val modelId: String? = null,
    val quantity: String = ""
)

/** Short one-line description for a collapsed requirement card, e.g. "Gateron Oil King × 90". */
fun SwitchRequirement.summaryText(): String {
    fun label(id: String?, options: List<FilterOption>): String? = id?.let { i ->
        if (FilterFieldValue.isCustomId(i)) FilterFieldValue.customLabel(i)
        else options.find { it.id == i }?.label
    }
    val brandLabel = label(brandId, SwitchCatalog.brands)
    val modelLabel = label(modelId, SwitchCatalog.modelsFor(brandId ?: ""))
    val name = listOfNotNull(brandLabel, modelLabel).joinToString(" ").ifBlank { "Switch" }
    return if (quantity.isNotBlank()) "$name × $quantity" else name
}

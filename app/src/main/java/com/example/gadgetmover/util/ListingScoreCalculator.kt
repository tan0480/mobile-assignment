package com.example.gadgetmover.util

import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.CategoryFilterSchema
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.FilterField
import com.example.gadgetmover.model.filter.FilterFieldValue
import com.example.gadgetmover.model.filter.effectiveOptions
import com.example.gadgetmover.model.filter.isFilled
import com.example.gadgetmover.model.filter.isVisible
import kotlin.math.roundToInt

/** "Other" / "Others" / "None" / "-" read as a real choice to [FilterFieldValue.isFilled], but they're not a *specific* spec — the same equivalence class [com.example.gadgetmover.model.filter.CategoryFilterMatching] treats as interchangeable when matching a buyer's filter. A listing that leaves every option-based field on one of these doesn't earn completeness credit for it. */
private val GENERIC_LABELS = setOf("other", "others", "none", "-")

private fun isGenericLabel(label: String?): Boolean = label != null && label.trim().lowercase() in GENERIC_LABELS

/** True if at least one of [ids] resolves (via [options]) to a real, specific value rather than a generic "Other"/"None"/"-" catalogue entry. A custom-typed id (see [FilterFieldValue.isCustomId]) is always specific — the seller wrote an actual value in, it just isn't in the catalogue. */
private fun hasSpecificSelection(ids: Set<String>, options: List<FilterOptionLookup>): Boolean =
    ids.any { id ->
        FilterFieldValue.isCustomId(id) || !isGenericLabel(options.find { it.id == id }?.label)
    }

private data class FilterOptionLookup(val id: String, val label: String)

/** Whether [value] counts toward completeness for [field]: filled per [FilterFieldValue.isFilled], and — for the option-based field shapes — not just a generic "Other"/"None"/"-" selection. Every other field shape (numbers, ranges, toggles, the repeatable requirement builders) has no such generic escape hatch, so [FilterFieldValue.isFilled] alone decides those. [state] resolves [FilterField.optionsForState]-narrowed catalogues (e.g. SoC Model narrowed by the seller's own SoC Brand pick) against the seller's *actual* other answers rather than an empty one. */
private fun countsAsComplete(value: FilterFieldValue?, field: FilterField, state: CategoryFilterState): Boolean {
    if (!value.isFilled(field)) return false
    val options = field.effectiveOptions(state).map { FilterOptionLookup(it.id, it.label) }
    return when (value) {
        is FilterFieldValue.MultiSelect -> hasSpecificSelection(value.selectedIds, options)
        is FilterFieldValue.SingleSelect -> value.selectedId?.let { hasSpecificSelection(setOf(it), options) } ?: false
        else -> true
    }
}

/** The result of scoring one listing's specification completeness — see [ListingScoreCalculator.score]. */
data class ListingCompletenessScore(
    val completenessRatio: Float,
    val filledFieldCount: Int,
    val totalFieldCount: Int
) {
    /** Rounded 0-100 for display (e.g. "Spec Completeness: 80%"). */
    val percent: Int get() = (completenessRatio * 100f).roundToInt()

    /** `1.0` at 0% complete, up to `1.5` (+50%) at 100% complete — see [ListingScoreCalculator.boostMultiplier]. */
    val boostMultiplier: Float get() = ListingScoreCalculator.boostMultiplier(completenessRatio)
}

/**
 * Scores how thoroughly a seller filled in a listing's category-specific technical
 * specifications, and turns that into a search/recommendation ranking boost — see
 * [screen.home.HomeScreen]'s "Recommended for you" ordering and [screen.explore.ExploreScreen]'s
 * default sort, both of which multiply their base ranking score by [boostMultiplier]. A category
 * with no registered schema (nothing in [CategoryFilterRegistry] to fill in) scores 0 fields
 * filled out of 0 total — [ListingCompletenessScore.completenessRatio] is 0f, not a divide-by-zero.
 */
object ListingScoreCalculator {

    /**
     * Only counts fields currently *visible* to the seller given their own other selections
     * (via [FilterField.isVisible]) — a field another of the seller's own answers hides (e.g.
     * "PCIe Generation" on an SSD listed as SATA rather than NVMe) was never fillable in the
     * first place, so it's excluded from the denominator rather than silently capping every SATA
     * listing's maximum possible score below 100%.
     */
    fun score(category: ProductCategory?, specs: CategoryFilterState): ListingCompletenessScore {
        val schema = category?.let { CategoryFilterRegistry.schemaFor(it) }
            ?: return ListingCompletenessScore(0f, 0, 0)
        return score(schema, specs)
    }

    /** Overload for a call site (the listing wizard's Specs step) that already resolved its own [CategoryFilterSchema], so it doesn't need a [ProductCategory] round-trip back through [CategoryFilterRegistry]. */
    fun score(schema: CategoryFilterSchema, specs: CategoryFilterState): ListingCompletenessScore {
        val applicableFields = schema.sections.filter { it.isVisible(specs) }
        val filledCount = applicableFields.count { field -> countsAsComplete(specs.valueFor(field.key), field, specs) }
        val totalCount = applicableFields.size
        val ratio = if (totalCount == 0) 0f else (filledCount.toFloat() / totalCount).coerceIn(0f, 1f)
        return ListingCompletenessScore(ratio, filledCount, totalCount)
    }

    fun score(product: Product): ListingCompletenessScore = score(product.category, product.specs)

    /** Convenience API for ranking call sites that only need the normalized completeness value. */
    fun calculateCompletenessRatio(product: Product): Float = score(product).completenessRatio

    /** `1.0` base, up to `1.0 + 0.5` (+50%) at full completeness — see item 1/3 of the listing-completeness spec this implements. */
    fun boostMultiplier(completenessRatio: Float): Float = 1.0f + (completenessRatio.coerceIn(0f, 1f) * 0.5f)
}

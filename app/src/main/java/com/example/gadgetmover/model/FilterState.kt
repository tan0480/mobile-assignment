package com.example.gadgetmover.model

import com.example.gadgetmover.model.filter.FloatRangeSerializer
import kotlinx.serialization.Serializable

/** A buyer's chosen "browse meet-up listings near me" center + radius — see [com.example.gadgetmover.screen.explore.LocationRadiusFilterScreen]. Only ever narrows [FulfillmentMethod.MEETUP] listings; a shipping-only listing has no meet-up spot to measure against, so it's left alone regardless of this filter. */
@Serializable
data class LocationRadiusFilter(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusKm: Float
)

@Serializable
enum class SortOption(val label: String) {
    RELEVANCE("Relevance"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    NEWEST("Newest First"),
    RATING("Top Rated Sellers")
}

// @Serializable so ExploreScreen can round-trip this through a rememberSaveable JSON Saver
// (needed to survive navigating into Product Detail and back without losing the in-progress
// search/filters) — not persisted anywhere server-side, this is still purely an in-memory
// query/UI object.
//
// Deliberately just the generic, category-agnostic quick filters (text query, listing type,
// category, condition, price, sort) — anything category-specific (switch type, layout,
// connectivity, battery life, ...) belongs in `CategoryFilterState`/`CategoryFilterRegistry`
// instead, which actually varies per category rather than assuming every product is a keyboard.
@Serializable
data class FilterState(
    val query: String = "",
    val transactionType: ListingType? = null,
    val categories: Set<ProductCategory> = emptySet(),
    val conditions: Set<Condition> = emptySet(),
    @Serializable(with = FloatRangeSerializer::class)
    val priceRange: ClosedFloatingPointRange<Float> = 0f..2000f,
    val sortBy: SortOption = SortOption.RELEVANCE,
    val locationFilter: LocationRadiusFilter? = null
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (transactionType != null) count++
            if (categories.isNotEmpty()) count++
            if (conditions.isNotEmpty()) count++
            if (priceRange != 0f..2000f) count++
            if (locationFilter != null) count++
            return count
        }
}

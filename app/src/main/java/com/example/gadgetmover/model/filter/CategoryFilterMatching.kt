package com.example.gadgetmover.model.filter

import com.example.gadgetmover.model.Condition
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.Product

/** Different category schemas phrase the same "nothing specific" concept differently — one field's catch-all option is "Other", another's is "None", another's is just "-". Resolving each to the same shared token here lets a buyer filtering by any one of them still match a product tagged with any of the others, instead of requiring an exact spelling match. */
private val OTHER_EQUIVALENT_LABELS = setOf("other", "others", "none", "-")
private const val OTHER_EQUIVALENT_TOKEN = "__other_equivalent__"

private fun canonicalId(id: String, options: List<FilterOption>): String {
    if (FilterFieldValue.isCustomId(id)) return id
    val label = options.find { it.id == id }?.label?.trim()?.lowercase()
    return if (label != null && label in OTHER_EQUIVALENT_LABELS) OTHER_EQUIVALENT_TOKEN else id
}

/** True if any of [productIds] is one of [buyerIds] — either a catalogue id (see [canonicalId] for the "Other"/"None"/"-" equivalence), or (for a [FilterType.SearchablePopupSelect] with free text typed in) a case-insensitive substring match against that id's option label. */
private fun multiSelectMatches(field: FilterField, buyerIds: Set<String>, productIds: Set<String>): Boolean {
    if (buyerIds.isEmpty()) return true
    val catalogueBuyerIds = buyerIds.filterNot { FilterFieldValue.isCustomId(it) }.map { canonicalId(it, field.options) }.toSet()
    val customBuyerLabels = buyerIds.filter { FilterFieldValue.isCustomId(it) }.map { FilterFieldValue.customLabel(it).lowercase() }
    if (productIds.any { canonicalId(it, field.options) in catalogueBuyerIds }) return true
    if (customBuyerLabels.isEmpty()) return false
    return productIds.any { pid ->
        val label = field.options.find { it.id == pid }?.label ?: pid
        customBuyerLabels.any { label.lowercase().contains(it) }
    }
}

/** Both [FilterFieldValue.MultiSelect] and [FilterFieldValue.SingleSelect] resolve to "the set of ids this side picked" so option-based fields can be compared uniformly regardless of which shape a given product/buyer value happens to be. */
private fun selectedIds(value: FilterFieldValue?): Set<String> = when (value) {
    is FilterFieldValue.MultiSelect -> value.selectedIds
    is FilterFieldValue.SingleSelect -> value.selectedId?.let { setOf(it) } ?: emptySet()
    else -> emptySet()
}

/**
 * Whether a product's stored [productValue] for [field] satisfies the buyer's [buyerValue] —
 * dispatched per [FilterFieldValue] shape so this works identically for every one of a category's
 * dozens of fields instead of a hand-picked few. [buyerValue] is already confirmed non-default
 * (see [applyCategoryFilterState]'s `isFilled` check) before this is called.
 */
private fun fieldMatches(field: FilterField, buyerValue: FilterFieldValue, productValue: FilterFieldValue?): Boolean {
    return when (buyerValue) {
        is FilterFieldValue.MultiSelect ->
            multiSelectMatches(field, buyerValue.selectedIds, selectedIds(productValue))

        is FilterFieldValue.SingleSelect -> {
            val buyerId = buyerValue.selectedId
            if (buyerId == null) {
                true
            } else {
                val canonicalBuyer = canonicalId(buyerId, field.options)
                selectedIds(productValue).any { canonicalId(it, field.options) == canonicalBuyer }
            }
        }

        is FilterFieldValue.RangeInput -> {
            val productRange = when (productValue) {
                is FilterFieldValue.RangeInput -> productValue.range
                // Listing mode stores a single NumberInput for NumberRange fields.
                is FilterFieldValue.NumberInput -> productValue.value.toFloatOrNull()?.let { it..it }
                else -> null
            }
            // The seller never specified this spec — don't exclude the product over it.
            productRange == null ||
                (productRange.start <= buyerValue.range.endInclusive && productRange.endInclusive >= buyerValue.range.start)
        }

        is FilterFieldValue.Toggle ->
            !buyerValue.enabled || (productValue as? FilterFieldValue.Toggle)?.enabled == true

        is FilterFieldValue.NumberInput -> {
            // No listing UI treats a NumberInput as a range, so a buyer's value is read as a floor.
            val buyerNum = buyerValue.value.toDoubleOrNull()
            val productNum = (productValue as? FilterFieldValue.NumberInput)?.value?.toDoubleOrNull()
            buyerNum == null || (productNum != null && productNum >= buyerNum)
        }

        is FilterFieldValue.UnitRangeInput -> {
            val units = (field.type as? FilterType.NumberRangeWithUnitToggle)?.units.orEmpty()
            fun toBase(unit: String, amount: Float): Float? =
                units.find { it.unit == unit }?.let { amount * it.toBaseMultiplier }

            val buyerStart = toBase(buyerValue.unit, buyerValue.range.start)
            val buyerEnd = toBase(buyerValue.unit, buyerValue.range.endInclusive)
            val productRangeBase = when (productValue) {
                is FilterFieldValue.UnitRangeInput -> {
                    val s = toBase(productValue.unit, productValue.range.start)
                    val e = toBase(productValue.unit, productValue.range.endInclusive)
                    if (s != null && e != null) s..e else null
                }
                // Listing mode stores a single UnitNumberInput for NumberRangeWithUnitToggle fields.
                is FilterFieldValue.UnitNumberInput -> productValue.value.toFloatOrNull()
                    ?.let { toBase(productValue.unit, it) }
                    ?.let { it..it }
                else -> null
            }
            // The seller never specified this spec — don't exclude the product over it.
            productRangeBase == null || buyerStart == null || buyerEnd == null ||
                (productRangeBase.start <= buyerEnd && productRangeBase.endInclusive >= buyerStart)
        }

        is FilterFieldValue.UnitNumberInput -> {
            // No listing UI treats a UnitNumberInput as a range, so a buyer's value is read as a floor (same unit-normalization as UnitRangeInput above).
            val units = (field.type as? FilterType.NumberRangeWithUnitToggle)?.units.orEmpty()
            fun toBase(unit: String, amount: Float): Float? =
                units.find { it.unit == unit }?.let { amount * it.toBaseMultiplier }

            val buyerNum = buyerValue.value.toFloatOrNull()?.let { toBase(buyerValue.unit, it) }
            val productNum = (productValue as? FilterFieldValue.UnitNumberInput)?.value?.toFloatOrNull()
                ?.let { toBase(productValue.unit, it) }
            buyerNum == null || (productNum != null && productNum >= buyerNum)
        }

        is FilterFieldValue.CameraRequirements -> {
            val productModules = (productValue as? FilterFieldValue.CameraRequirements)?.items.orEmpty().map { it.toActualCameraModule() }
            buyerValue.items.satisfiesAllCameraRequirements(productModules)
        }

        is FilterFieldValue.PcieSlotRequirements -> {
            val productSlots = (productValue as? FilterFieldValue.PcieSlotRequirements)?.items.orEmpty().map { it.toActualPcieSlot() }
            buyerValue.items.satisfiesAllPcieSlotRequirements(productSlots)
        }

        is FilterFieldValue.SwitchRequirements -> {
            val productItems = (productValue as? FilterFieldValue.SwitchRequirements)?.items.orEmpty()
            buyerValue.items.all { req ->
                productItems.any { item ->
                    (req.brandId == null || req.brandId == item.brandId) &&
                        (req.modelId == null || req.modelId == item.modelId)
                }
            }
        }

        is FilterFieldValue.VideoPortRequirements -> {
            val productItems = (productValue as? FilterFieldValue.VideoPortRequirements)?.items.orEmpty()
            buyerValue.items.all { req ->
                productItems.any { item -> req.portTypeId == null || req.portTypeId == item.portTypeId }
            }
        }
    }
}

/**
 * Narrows [this] by every filter the buyer has actually set in [state], generically comparing
 * each of [schema]'s fields' buyer-selected value against the product's own stored value for that
 * same key (`product.specs`) — now that a seller's listing wizard fills in exactly the same
 * [CategoryFilterState] shape a buyer filters with (see `ListingWizardScreen`), every field
 * round-trips instead of only the handful this used to special-case. `price` and `condition` are
 * still handled separately since they read real, non-`specs` [Product] fields.
 */
fun List<Product>.applyCategoryFilterState(state: CategoryFilterState, schema: CategoryFilterSchema): List<Product> {
    var results = this

    (state.valueFor("price") as? FilterFieldValue.RangeInput)?.let { value ->
        val range = value.range
        results = results.filter { product ->
            val effectivePrice = product.price ?: product.rentalRatePerDay ?: 0.0
            effectivePrice >= range.start && effectivePrice <= range.endInclusive
        }
    }

    (state.valueFor("condition") as? FilterFieldValue.MultiSelect)?.selectedIds
        ?.takeIf { it.isNotEmpty() }
        ?.let { ids ->
            val conditions = ids.mapNotNull { id -> runCatching { Condition.valueOf(id.uppercase()) }.getOrNull() }
            if (conditions.isNotEmpty()) results = results.filter { it.condition in conditions }
        }

    (state.valueFor("fulfillment_method") as? FilterFieldValue.MultiSelect)?.selectedIds
        ?.takeIf { it.isNotEmpty() }
        ?.let { ids ->
            val methods = ids.mapNotNull { id -> runCatching { FulfillmentMethod.valueOf(id) }.getOrNull() }
            if (methods.isNotEmpty()) results = results.filter { product -> product.fulfillmentMethods.any { it in methods } }
        }

    (state.valueFor("seller_state") as? FilterFieldValue.SingleSelect)?.selectedId?.let { selectedState ->
        results = results.filter { it.sellerState == selectedState }
    }

    for (field in schema.sections) {
        if (field.key == "price" || field.key == "condition" || field.key == "fulfillment_method" || field.key == "seller_state") continue
        val buyerValue = state.valueFor(field.key)
        if (!buyerValue.isFilled(field)) continue
        results = results.filter { product -> fieldMatches(field, buyerValue!!, product.specs.valueFor(field.key)) }
    }

    return results
}

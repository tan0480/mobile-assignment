package com.example.gadgetmover.model.filter

import com.example.gadgetmover.model.ProductCategory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * One selectable value inside a [FilterType.ChipGroup], [FilterType.SearchablePopupSelect],
 * [FilterType.CheckboxList], or [FilterType.RadioGroup] field. [id] is a stable, storage-safe
 * slug; [label] is what's shown in the UI. Kept separate from any enum so every category's
 * option list — including the ~50-entry switch model catalogue — can be plain data instead of
 * requiring a matching Kotlin enum per field.
 */
data class FilterOption(
    val id: String,
    val label: String
)

/** Which Compose widget a [FilterField] renders as, carrying that widget's own configuration. */
sealed class FilterType {
    data class ChipGroup(val isMultiSelect: Boolean) : FilterType()
    data class SearchablePopupSelect(val isMultiSelect: Boolean, val allowCustomInput: Boolean) : FilterType()
    data object CheckboxList : FilterType()
    data object RadioGroup : FilterType()
    data class NumberInputField(val placeholder: String, val unit: String) : FilterType()
    data class NumberRange(val min: Float, val max: Float, val step: Float, val unit: String, val unitIsPrefix: Boolean = true) : FilterType()
    /** One unit's bounds within a [NumberRangeWithUnitToggle] — [toBaseMultiplier] converts a value in [unit] to the type's common base unit (e.g. GB) so a buyer filtering in one unit still matches a listing entered in the other. */
    data class UnitRange(val unit: String, val min: Float, val max: Float, val step: Float, val toBaseMultiplier: Float)
    /** Like [NumberRange], but the seller/buyer first picks which of [units] they're entering a value in (e.g. GB vs TB storage capacity) — see [UnitRange.toBaseMultiplier] for how the two stay comparable. */
    data class NumberRangeWithUnitToggle(val units: List<UnitRange>) : FilterType()
    data class SwitchToggle(val label: String) : FilterType()
    /** A repeatable "Add Camera" requirement builder — see [CameraRequirement] — rather than a fixed field per camera slot. */
    data object CameraSystemBuilder : FilterType()
    /** A repeatable "Add PCIe Slot" requirement builder — see [PcieSlotRequirement] — for motherboards that need e.g. a PCIe 5.0 x16 AND a PCIe 4.0 x1 slot at once. */
    data object PcieSlotBuilder : FilterType()
    /** A repeatable "Add Switch" brand+model+quantity builder — see [SwitchRequirement] — for keyboards built with more than one switch type. */
    data object SwitchSystemBuilder : FilterType()
    /** A repeatable "Add Port" type+quantity builder — see [VideoPortRequirement] — replacing a fixed field per port generation with one flat catalogue a monitor's actual port mix is built from. */
    data object VideoPortBuilder : FilterType()
}

/**
 * A [FilterField] is only shown once another field already has one of [anyOfOptionIds] selected —
 * e.g. a "Polling Rate (Bluetooth)" field that only makes sense once "Bluetooth" is picked under
 * Connectivity. [fieldKey] refers to another [FilterField.key] within the same schema. Used for
 * both [FilterField.visibleWhen] (shown only if matched) and [FilterField.hiddenWhen] (hidden if
 * matched) — e.g. a panoramic-glass case has no separate "Side Panel Material" to pick.
 */
data class FieldDependency(val fieldKey: String, val anyOfOptionIds: Set<String>)

/** One filterable attribute (e.g. "Switch Type") within a category's advanced filter schema. */
data class FilterField(
    val key: String,
    val label: String,
    val type: FilterType,
    val options: List<FilterOption> = emptyList(),
    val visibleWhen: FieldDependency? = null,
    /** The inverse of [visibleWhen] — this field disappears once the dependency matches, instead of only appearing once it does. A field can only use one of the two. */
    val hiddenWhen: FieldDependency? = null,
    /** When set, overrides [options] with a list computed from another field's current selection — e.g. SoC Model narrowing to just the picked SoC Brand's chips, or OS Version narrowing to the picked OS. Falls back to [options] when the dependency has nothing selected. */
    val optionsForState: ((CategoryFilterState) -> List<FilterOption>)? = null
)

/** The options a field should actually show right now: [FilterField.optionsForState] applied to [state] if the field has one, otherwise its static [FilterField.options]. */
fun FilterField.effectiveOptions(state: CategoryFilterState): List<FilterOption> =
    optionsForState?.invoke(state) ?: options

/** The ids currently selected for [fieldKey] in [state], regardless of whether that field is a [FilterFieldValue.MultiSelect] or a [FilterFieldValue.SingleSelect] — shared by [isVisible] and by every schema's `optionsForState` narrowing closure (e.g. SoC Model narrowing to SoC Brand), so a field can switch between single- and multi-select without breaking whatever else reads its current selection. */
fun selectedIdsFor(state: CategoryFilterState, fieldKey: String): Set<String> =
    when (val value = state.valueFor(fieldKey)) {
        is FilterFieldValue.MultiSelect -> value.selectedIds
        is FilterFieldValue.SingleSelect -> value.selectedId?.let { setOf(it) } ?: emptySet()
        is FilterFieldValue.VideoPortRequirements -> value.items.mapNotNull { it.portTypeId }.toSet()
        else -> emptySet()
    }

/** Whether [this] field should currently be shown, given the schema's in-progress [state]. Fields with neither [FilterField.visibleWhen] nor [FilterField.hiddenWhen] are always visible. */
fun FilterField.isVisible(state: CategoryFilterState): Boolean {
    visibleWhen?.let { dependency ->
        if (dependency.anyOfOptionIds.none { it in selectedIdsFor(state, dependency.fieldKey) }) return false
    }
    hiddenWhen?.let { dependency ->
        if (dependency.anyOfOptionIds.any { it in selectedIdsFor(state, dependency.fieldKey) }) return false
    }
    return true
}

/**
 * The complete set of category-specific advanced filters for one [ProductCategory], including
 * that category's own `brand` field — unlike Price/Condition, Brand's actual option catalogue is
 * different for every category (headphone brands aren't laptop brands), so it lives here rather
 * than in [CommonFilterFields].
 */
data class CategoryFilterSchema(
    val sections: List<FilterField>
)

/**
 * (De)serializes a [ClosedFloatingPointRange]`<Float>` through a plain `{start, endInclusive}`
 * surrogate — the range type itself has no built-in kotlinx.serialization support, and this
 * keeps every existing call site that reads `.range`/`.start`/`.endInclusive` untouched.
 * `internal` (not `private`) so [com.example.gadgetmover.model.FilterState] can reuse it too.
 */
internal object FloatRangeSerializer : KSerializer<ClosedFloatingPointRange<Float>> {
    @Serializable
    private data class Surrogate(val start: Float, val endInclusive: Float)

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ClosedFloatingPointRange<Float>) {
        encoder.encodeSerializableValue(Surrogate.serializer(), Surrogate(value.start, value.endInclusive))
    }

    override fun deserialize(decoder: Decoder): ClosedFloatingPointRange<Float> {
        val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())
        return surrogate.start..surrogate.endInclusive
    }
}

/**
 * The user's current selection for a single [FilterField], shaped to match its [FilterType].
 * `@Serializable` (with a stable [SerialName] per variant, so a future Kotlin class rename can't
 * silently change the JSON discriminator of an already-stored [CategoryFilterState]) so a whole
 * [CategoryFilterState] can round-trip through Supabase's `products.specs` jsonb column.
 */
@Serializable
sealed class FilterFieldValue {
    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(val selectedIds: Set<String> = emptySet()) : FilterFieldValue()

    @Serializable
    @SerialName("single_select")
    data class SingleSelect(val selectedId: String? = null) : FilterFieldValue()

    @Serializable
    @SerialName("number_input")
    data class NumberInput(val value: String = "") : FilterFieldValue()

    @Serializable
    @SerialName("range_input")
    data class RangeInput(@Serializable(with = FloatRangeSerializer::class) val range: ClosedFloatingPointRange<Float>) : FilterFieldValue()

    /** Listing-mode value for a [FilterType.NumberRangeWithUnitToggle] — one exact number plus which of the field's units it's entered in. */
    @Serializable
    @SerialName("unit_number_input")
    data class UnitNumberInput(val unit: String = "", val value: String = "") : FilterFieldValue()

    /** Filter-mode value for a [FilterType.NumberRangeWithUnitToggle]. */
    @Serializable
    @SerialName("unit_range_input")
    data class UnitRangeInput(val unit: String = "", @Serializable(with = FloatRangeSerializer::class) val range: ClosedFloatingPointRange<Float> = 0f..0f) : FilterFieldValue()

    @Serializable
    @SerialName("toggle")
    data class Toggle(val enabled: Boolean = false) : FilterFieldValue()

    /** The user-built list of [CameraRequirement] slots behind [FilterType.CameraSystemBuilder] — unlike every other case here, one field holds a variable-length list of independent sub-objects instead of a single value. */
    @Serializable
    @SerialName("camera_requirements")
    data class CameraRequirements(val items: List<CameraRequirement> = emptyList()) : FilterFieldValue()

    /** The user-built list of [PcieSlotRequirement] slots behind [FilterType.PcieSlotBuilder]. */
    @Serializable
    @SerialName("pcie_slot_requirements")
    data class PcieSlotRequirements(val items: List<PcieSlotRequirement> = emptyList()) : FilterFieldValue()

    /** The user-built list of [SwitchRequirement] slots behind [FilterType.SwitchSystemBuilder]. */
    @Serializable
    @SerialName("switch_requirements")
    data class SwitchRequirements(val items: List<SwitchRequirement> = emptyList()) : FilterFieldValue()

    /** The user-built list of [VideoPortRequirement] slots behind [FilterType.VideoPortBuilder]. */
    @Serializable
    @SerialName("video_port_requirements")
    data class VideoPortRequirements(val items: List<VideoPortRequirement> = emptyList()) : FilterFieldValue()

    companion object {
        /** A stable id for a custom, not-in-the-catalogue value typed into a
         * [FilterType.SearchablePopupSelect] with `allowCustomInput = true`. */
        fun customId(text: String): String = "custom:$text"
        fun isCustomId(id: String): Boolean = id.startsWith("custom:")
        fun customLabel(id: String): String = id.removePrefix("custom:")
    }
}

/**
 * Whether [this] value counts as "the seller/buyer actually filled this field in", using [field]
 * to know its type-specific default (e.g. a [FilterType.NumberRange]'s untouched min..max). Shared
 * by [CategoryFilterState.activeFieldCount] (the filter-sheet badge count), the listing wizard's
 * required-Brand check, and the read-only product-detail renderer (which shows a field only when
 * it's filled).
 */
fun FilterFieldValue?.isFilled(field: FilterField): Boolean = when (this) {
    null -> false
    is FilterFieldValue.MultiSelect -> selectedIds.isNotEmpty()
    is FilterFieldValue.SingleSelect -> selectedId != null
    is FilterFieldValue.NumberInput -> value.isNotBlank()
    is FilterFieldValue.Toggle -> enabled
    is FilterFieldValue.RangeInput -> {
        val range = field.type as? FilterType.NumberRange
        range != null && (this.range.start != range.min || this.range.endInclusive != range.max)
    }
    is FilterFieldValue.UnitNumberInput -> value.isNotBlank()
    is FilterFieldValue.UnitRangeInput -> {
        val active = (field.type as? FilterType.NumberRangeWithUnitToggle)?.units?.find { it.unit == unit }
        active != null && (this.range.start != active.min || this.range.endInclusive != active.max)
    }
    is FilterFieldValue.CameraRequirements -> items.isNotEmpty()
    is FilterFieldValue.PcieSlotRequirements -> items.isNotEmpty()
    is FilterFieldValue.SwitchRequirements -> items.isNotEmpty()
    is FilterFieldValue.VideoPortRequirements -> items.isNotEmpty()
}

/**
 * Whether [this] value is in-bounds for [field] — catches the same out-of-range number the
 * listing wizard's spec step already flags inline (see `NumberInputFieldWidget`'s error state in
 * `DynamicFilterField`): a listing-mode [FilterType.NumberRange] value must be greater than 0 and
 * under 10x the field's upper bound. Every other field shape has no such bound, so this is always
 * true for those — it only ever turns false for the one shape that can actually be "filled in but
 * still wrong." Used to gate the listing wizard's Continue button so an invalid number can't be
 * carried forward silently.
 */
fun FilterFieldValue?.isValidForListing(field: FilterField): Boolean {
    if (this is FilterFieldValue.UnitNumberInput) {
        val active = (field.type as? FilterType.NumberRangeWithUnitToggle)?.units?.find { it.unit == unit } ?: return true
        val entered = value.toFloatOrNull() ?: return true
        val upperBound = active.max * 10
        return entered > 0f && entered < upperBound
    }
    if (this !is FilterFieldValue.NumberInput) return true
    val type = field.type as? FilterType.NumberRange ?: return true
    val entered = value.toFloatOrNull() ?: return true
    val upperBound = type.max * 10
    return entered > 0f && entered < upperBound
}

/**
 * Holds a category's field selections, keyed by [FilterField.key] — either a buyer's in-progress
 * advanced-filter draft, or (reusing the exact same shape) a seller's filled-in product specs,
 * stored as-is in Supabase's `products.specs` jsonb column.
 */
@Serializable
data class CategoryFilterState(
    val values: Map<String, FilterFieldValue> = emptyMap()
) {
    fun valueFor(key: String): FilterFieldValue? = values[key]

    fun with(key: String, value: FilterFieldValue): CategoryFilterState =
        copy(values = values + (key to value))

    fun cleared(): CategoryFilterState = CategoryFilterState()

    /** Counts fields with a non-default selection, using [schema] to know each field's default (e.g. a [FilterType.NumberRange]'s min..max). */
    fun activeFieldCount(schema: CategoryFilterSchema): Int = schema.sections.count { field ->
        values[field.key].isFilled(field)
    }
}

/** Filters every category shares, so they're modeled once instead of being repeated per category schema. */
object CommonFilterFields {
    val condition = FilterField(
        key = "condition",
        label = "Condition",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = listOf(
            FilterOption("new", "New"),
            FilterOption("like_new", "Like New"),
            FilterOption("good", "Good"),
            FilterOption("fair", "Fair")
        )
    )

    val price = FilterField(
        key = "price",
        label = "Price",
        type = FilterType.NumberRange(min = 0f, max = 5000f, step = 50f, unit = "RM")
    )

    /** Whether a listing supports shipping and/or meet-up — a real [com.example.gadgetmover.model.Product.fulfillmentMethods] field, matched the same way as [price]/[condition] rather than living inside `specs`. */
    val fulfillmentMethod = FilterField(
        key = "fulfillment_method",
        label = "Delivery / Meet-up",
        type = FilterType.ChipGroup(isMultiSelect = true),
        options = listOf(
            FilterOption("SHIPPING", "Shipping"),
            FilterOption("MEETUP", "Meet-up / Self-pickup")
        )
    )

    /** Matched against [com.example.gadgetmover.model.Product.sellerState] — the seller's own state, opportunistically resolved from a picked meet-up location during listing creation (see `util/SellerLocationResolver.kt`) — rather than anything in `specs`, same as [price]/[condition]/[fulfillmentMethod]. */
    val sellerState = FilterField(
        key = "seller_state",
        label = "State",
        type = FilterType.ChipGroup(isMultiSelect = false),
        options = MalaysiaStates.ALL.map { FilterOption(it, it) }
    )

    val fields: List<FilterField> = listOf(price, condition, fulfillmentMethod, sellerState)
}

/** Looks up the advanced filter schema for a given category. Categories with no entry yet fall back to `null` — the caller should hide the advanced-filters section rather than crash. */
object CategoryFilterRegistry {
    private val schemas: Map<ProductCategory, CategoryFilterSchema> = mapOf(
        ProductCategory.KEYBOARD to KeyboardFilterSchema.schema,
        ProductCategory.MOUSE to MiceFilterSchema.schema,
        ProductCategory.HEADPHONE to HeadphoneFilterSchema.schema,
        ProductCategory.WIRED_EARPHONE to WiredEarphoneFilterSchema.schema,
        ProductCategory.WIRELESS_EARPHONE to WirelessEarphoneFilterSchema.schema,
        ProductCategory.AUDIO to AudioSpeakerFilterSchema.schema,
        ProductCategory.LAPTOP to LaptopFilterSchema.schema,
        ProductCategory.MONITOR to MonitorFilterSchema.schema,
        ProductCategory.SMARTPHONE to PhoneFilterSchema.schema,
        ProductCategory.TABLET to TabletFilterSchema.schema,
        ProductCategory.ACCESSORY to AccessoryFilterSchema.schema,
        ProductCategory.GRAPHICS_CARD to GraphicsCardFilterSchema.schema,
        ProductCategory.CPU to CpuFilterSchema.schema,
        ProductCategory.MOTHERBOARD to MotherboardFilterSchema.schema,
        ProductCategory.RAM to RamFilterSchema.schema,
        ProductCategory.SSD to SsdFilterSchema.schema,
        ProductCategory.HDD to HddFilterSchema.schema,
        ProductCategory.PSU to PsuFilterSchema.schema,
        ProductCategory.PC_CASE to PcCaseFilterSchema.schema,
        ProductCategory.CPU_COOLER to CpuCoolerFilterSchema.schema,
        ProductCategory.CASE_FAN to CaseFanFilterSchema.schema
    )

    fun schemaFor(category: ProductCategory): CategoryFilterSchema? = schemas[category]
}

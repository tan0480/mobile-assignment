package com.example.gadgetmover.model

import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.displayText
import kotlinx.serialization.Serializable

@Serializable
enum class ListingType(val label: String) {
    BUY("Sell"),
    RENT("Rent It Out"),
    BOTH("Sell and Rent")
}

@Serializable
enum class ProductCategory(val label: String) {
    KEYBOARD("Keyboards"),
    HEADPHONE("Headphones"),
    AUDIO("Audio & Speakers"),
    MOUSE("Mice"),
    WIRED_EARPHONE("Wired Earphones"),
    WIRELESS_EARPHONE("Wireless Earphones"),
    SMARTPHONE("Smartphones"),
    TABLET("Tablets"),
    LAPTOP("Laptops"),
    MONITOR("Monitors"),
    GRAPHICS_CARD("Graphics Cards"),
    CPU("Processors (CPU)"),
    MOTHERBOARD("Motherboards"),
    RAM("Memory (RAM)"),
    SSD("Solid State Drives (SSD)"),
    HDD("Hard Disk Drives (HDD)"),
    PSU("Power Supplies (PSU)"),
    PC_CASE("PC Cases"),
    CPU_COOLER("CPU Coolers"),
    CASE_FAN("Case Fans"),
    ACCESSORY("Accessories")
}

@Serializable
enum class ProductStatus {
    AVAILABLE, SOLD
}

@Serializable
enum class Condition(val label: String) {
    NEW("New"),
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair")
}

@Serializable
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val category: ProductCategory,
    val listingType: ListingType,
    val price: Double? = null,
    val rentalRatePerDay: Double? = null,
    val deposit: Double? = null,
    val condition: Condition,
    /** The category's schema-driven fields as the seller filled them in — see `model/filter/FilterSchema.kt`. */
    val specs: CategoryFilterState = CategoryFilterState(),
    val images: List<String>,
    val sellerId: String,
    val sellerName: String,
    val sellerRating: Float,
    val sellerAvatarUrl: String = "",
    val location: String,
    val postedDate: String,
    val isFeatured: Boolean = false,
    val hasWarranty: Boolean = false,
    val warrantyDetails: String? = null,
    val isSaved: Boolean = false,
    val isSellerVerified: Boolean = false,
    /** Which handover methods this listing supports — checked at checkout, filterable via [com.example.gadgetmover.model.filter.CommonFilterFields.fulfillmentMethod]. */
    val fulfillmentMethods: Set<FulfillmentMethod> = emptySet(),
    /** Only meaningful when [FulfillmentMethod.MEETUP] is in [fulfillmentMethods]. */
    val meetupLocations: List<MeetupLocation> = emptyList(),
    /** Seller-set shipping fees for this listing — null means the seller doesn't offer that speed, even when [FulfillmentMethod.SHIPPING] is in [fulfillmentMethods]. */
    val standardShippingFee: Double? = null,
    val expressShippingFee: Double? = null,
    val status: ProductStatus = ProductStatus.AVAILABLE,
    /**
     * For a RENT/BOTH listing with SHIPPING enabled — the seller's own address a renter should
     * ship the item back to, snapshotted at listing time (set in the listing wizard, resolved
     * from [com.example.gadgetmover.data.AddressRepository] at save time). Null when not
     * applicable, same as the receiving-leg address snapshot on [CheckoutDetails].
     */
    val returnReceiverName: String? = null,
    val returnPhoneNumber: String? = null,
    val returnFullAddress: String? = null
) {
    /** Every category schema defines its own `brand` field (see `CategoryFilterRegistry`) — resolves whatever the seller picked there to a display string, e.g. for search/card text. Empty if unset. */
    fun brandLabel(): String {
        val brandField = CategoryFilterRegistry.schemaFor(category)?.sections?.find { it.key == "brand" } ?: return ""
        val value = specs.valueFor("brand") ?: return ""
        return value.displayText(brandField, specs)
    }
}

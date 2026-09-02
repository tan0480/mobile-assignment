package com.example.gadgetmover.util

import com.example.gadgetmover.model.ListingType

/** Pure validation shared by the listing UI and unit tests. Invalid text is never converted to null. */
fun validateListingNumbers(
    listingType: ListingType,
    price: String,
    rentalRate: String,
    deposit: String,
    shippingEnabled: Boolean,
    standardShippingFee: String,
    expressShippingFee: String
): Boolean {
    fun positive(text: String): Boolean = text.trim().toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    fun nonNegativeOrBlank(text: String): Boolean = text.isBlank() || text.trim().toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true

    val saleValid = when (listingType) {
        ListingType.BUY, ListingType.BOTH -> positive(price)
        ListingType.RENT -> price.isBlank()
    }
    val rentalValid = when (listingType) {
        ListingType.RENT, ListingType.BOTH -> positive(rentalRate)
        ListingType.BUY -> rentalRate.isBlank()
    }
    val depositValid = when (listingType) {
        ListingType.RENT, ListingType.BOTH -> deposit.isNotBlank() && nonNegativeOrBlank(deposit)
        ListingType.BUY -> deposit.isBlank()
    }
    val shippingValid = !shippingEnabled || (
        (standardShippingFee.isNotBlank() || expressShippingFee.isNotBlank()) &&
            nonNegativeOrBlank(standardShippingFee) && nonNegativeOrBlank(expressShippingFee)
        )
    return saleValid && rentalValid && depositValid && shippingValid
}

fun parseListingNumber(text: String, allowZero: Boolean = false): Double? =
    text.trim().toDoubleOrNull()?.takeIf { it.isFinite() && (if (allowZero) it >= 0 else it > 0) }

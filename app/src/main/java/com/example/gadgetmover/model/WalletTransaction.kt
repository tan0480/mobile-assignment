package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
enum class WalletTransactionType(val label: String) {
    DEPOSIT("Added funds"),
    WITHDRAWAL("Withdrawal"),
    /** Paying for an order out of wallet balance instead of a card — see [CheckoutPaymentMethod.WALLET]. */
    PURCHASE("Order payment"),
    SALE_PAYOUT("Sale payout"),
    RENTAL_PAYOUT("Rental payout"),
    DEPOSIT_REFUND("Rental Deposit Refund"),
    REFUND("Refund")
}

@Serializable
data class WalletTransaction(
    val id: String,
    val type: WalletTransactionType,
    val amount: Double,
    val description: String,
    val date: String
)

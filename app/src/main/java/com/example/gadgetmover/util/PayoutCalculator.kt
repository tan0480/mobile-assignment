package com.example.gadgetmover.util

import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.RentalOrder

/**
 * Client-side preview of the seller's/owner's payout for an order, shown as an "on hold" banner
 * before the funds are actually released. Must mirror the SQL `release_order_payout` trigger's
 * formula exactly (`total_amount - deposit_amount - platformFee`) — the trigger is the source of
 * truth for the real transaction amount, this is only an estimate for display.
 */
fun estimatedPayout(order: Order): Double = when (order) {
    is BuyOrder -> order.price - order.checkout.platformFee
    is RentalOrder -> order.totalAmount - order.deposit - order.checkout.platformFee
}

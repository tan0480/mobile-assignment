package com.example.gadgetmover.screen.checkout

/**
 * Converts transport/payment failures into messages safe to show in the UI.
 *
 * Ktor/Supabase exceptions can contain URLs, response bodies, and request
 * metadata. Those details are useful for developer logs, but must never be
 * rendered in a customer-facing dialog.
 */
internal fun checkoutUserMessage(error: Throwable?, fallback: String): String {
    val diagnostic = error?.message.orEmpty().lowercase()
    return if (diagnostic.contains("requested function was not found") ||
        diagnostic.contains("not_found") && diagnostic.contains("function")) {
        "Checkout service is temporarily unavailable. Please try again later."
    } else {
        fallback
    }
}

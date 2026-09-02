package com.example.gadgetmover.util

/**
 * Fixed courier list for the "Mark as Shipped" flow. Named carriers' [trackingPattern]s are
 * reasonable approximations of their real tracking-number formats (not independently verifiable
 * without each carrier's own docs) — refine later if a real number gets rejected. [OTHER]'s
 * pattern is the one explicitly specified by product: 8-30 alphanumeric characters.
 */
enum class Courier(val label: String, val trackingPattern: Regex) {
    JNT_EXPRESS("J&T Express", Regex("^JT\\d{9,13}$")),
    POS_LAJU("Pos Laju", Regex("^[A-Z]{2}\\d{9}MY$")),
    NINJA_VAN("Ninja Van", Regex("^[A-Z0-9]{10,15}$")),
    DHL_ECOMMERCE("DHL eCommerce", Regex("^\\d{10,12}$")),
    CITY_LINK_EXPRESS("City-Link Express", Regex("^CL\\d{8,10}$")),
    SKYNET("Skynet", Regex("^SKY\\d{8,10}$")),
    OTHER("Other", Regex("^[A-Za-z0-9]{8,30}$"))
}

fun Courier.validate(trackingNumber: String): Boolean = trackingPattern.matches(trackingNumber.trim())

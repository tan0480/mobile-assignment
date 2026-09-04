package com.example.gadgetmover.util

/** Formats an amount as Malaysian Ringgit, e.g. `formatMoney(620.0) == "RM620"`. */
fun formatMoney(amount: Double): String = "RM${"%.0f".format(amount)}"

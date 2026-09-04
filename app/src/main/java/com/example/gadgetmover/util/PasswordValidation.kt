package com.example.gadgetmover.util

/** Requirement copy shown next to any password field. */
const val PASSWORD_REQUIREMENTS_HINT =
    "Password must be 8-20 characters and include at least 3 of: uppercase, lowercase, a number, a symbol."

/**
 * Returns null if [password] satisfies the app's password policy (8-20 chars, at least 3 of
 * the 4 character categories: uppercase, lowercase, digit, symbol), otherwise returns
 * [PASSWORD_REQUIREMENTS_HINT] to show as a validation error.
 */
fun validatePassword(password: String): String? {
    val categoriesMet = listOf(
        password.any { it.isUpperCase() },
        password.any { it.isLowerCase() },
        password.any { it.isDigit() },
        password.any { !it.isLetterOrDigit() }
    ).count { it }
    val meetsPolicy = password.length in 8..20 && categoriesMet >= 3
    return if (meetsPolicy) null else PASSWORD_REQUIREMENTS_HINT
}

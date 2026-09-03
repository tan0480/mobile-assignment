package com.example.gadgetmover.util

/** A country calling code option shown in the phone number field's country picker. */
data class CountryCode(val countryName: String, val flagEmoji: String, val dialCode: String, val localDigits: IntRange)

val countryCodes = listOf(
    CountryCode("United States", "🇺🇸", "+1", 10..10),
    CountryCode("Canada", "🇨🇦", "+1", 10..10),
    CountryCode("Malaysia", "🇲🇾", "+60", 9..10),
    CountryCode("Singapore", "🇸🇬", "+65", 8..8),
    CountryCode("United Kingdom", "🇬🇧", "+44", 10..10),
    CountryCode("Australia", "🇦🇺", "+61", 9..9),
    CountryCode("China", "🇨🇳", "+86", 11..11),
    CountryCode("India", "🇮🇳", "+91", 10..10),
    CountryCode("Indonesia", "🇮🇩", "+62", 9..12),
    CountryCode("Japan", "🇯🇵", "+81", 9..10),
    CountryCode("South Korea", "🇰🇷", "+82", 9..10)
)

val defaultCountryCode = countryCodes.first()

/** True if [localNumber] (digits only, no dial code) matches [country]'s expected mobile number length. */
fun isValidPhoneNumber(country: CountryCode, localNumber: String): Boolean =
    localNumber.isNotEmpty() && localNumber.all { it.isDigit() } && localNumber.length in country.localDigits

/**
 * Splits a stored phone string like "+60 12-345 6789" into its country code and local digits,
 * for prefilling the country-code phone field when editing an existing number.
 */
fun parsePhoneNumber(stored: String): Pair<CountryCode, String> {
    val digitsWithPlus = stored.trim()
    val matchedCountry = countryCodes
        .sortedByDescending { it.dialCode.length }
        .firstOrNull { digitsWithPlus.startsWith(it.dialCode) }
        ?: defaultCountryCode
    val remainder = digitsWithPlus.removePrefix(matchedCountry.dialCode)
    return matchedCountry to remainder.filter { it.isDigit() }
}

private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/** True if [email] is a plausible, well-formed email address. */
fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

private val userIdRegex = Regex("^[a-z0-9_]{3,20}$")

/** True if [userId] is a well-formed public handle: 3-20 lowercase letters, digits, or underscores. */
fun isValidUserId(userId: String): Boolean = userIdRegex.matches(userId)

/** Strips [raw] down to what [isValidUserId] allows, lowercasing as it goes — used to sanitize a user ID field as the user types it. */
fun sanitizeUserIdInput(raw: String): String = raw.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }.take(20)

/**
 * Strips [raw] down to a valid RM amount as the user types: digits and at most one decimal
 * point, with the fractional part capped at 2 digits. Any extra "." characters are dropped
 * rather than rejected, so pasting something like "12.3.4" collapses to "12.34".
 */
fun sanitizeMoneyInput(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val dotIndex = filtered.indexOf('.')
    if (dotIndex == -1) return filtered
    val wholePart = filtered.substring(0, dotIndex)
    val fractionalPart = filtered.substring(dotIndex + 1).filter { it.isDigit() }.take(2)
    return "$wholePart.$fractionalPart"
}

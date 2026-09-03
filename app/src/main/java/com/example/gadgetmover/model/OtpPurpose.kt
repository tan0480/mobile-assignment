package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
enum class OtpPurpose {
    REGISTRATION,
    FORGOT_PASSWORD
}

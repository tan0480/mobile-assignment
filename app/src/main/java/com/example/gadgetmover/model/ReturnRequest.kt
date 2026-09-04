package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReturnRequestType { RETURN, REFUND }

@Serializable
enum class ReturnMethod { MEETUP, SHIPPING }

@Serializable
enum class ReturnRequestStatus { PENDING, ACCEPTED, REJECTED }

/** Preset reasons offered on the return/refund request form — the last one ("Other") always pairs with a free-text field. */
val returnRequestReasons = listOf(
    "Item damaged",
    "Item not as described",
    "Wrong item received",
    "Missing parts or accessories",
    "Changed my mind",
    "Other"
)

@Serializable
data class ReturnRequest(
    val id: String,
    val orderId: String,
    val requesterId: String,
    val attemptNumber: Int,
    val requestType: ReturnRequestType,
    val reasonCode: String,
    val reasonOtherText: String = "",
    val refundAmount: Double? = null,
    val returnMethod: ReturnMethod? = null,
    /** The buyer's suggested meet-up spot — context for the seller, not authoritative; the seller picks the final location when accepting. */
    val meetupLocation: MeetupLocation? = null,
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val status: ReturnRequestStatus = ReturnRequestStatus.PENDING,
    val rejectionReason: String? = null,
    val createdDate: String = ""
)

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
    /** Every send-back method the buyer is personally OK with — the seller then picks exactly one final method from this set when accepting. */
    val returnMethods: Set<ReturnMethod> = emptySet(),
    /** The buyer's whole candidate set of acceptable meet-up spots (only meaningful when [ReturnMethod.MEETUP] is in [returnMethods]) — the seller picks exactly one final location from this set, not from the listing's own declared spots. */
    val meetupLocations: List<MeetupLocation> = emptyList(),
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val status: ReturnRequestStatus = ReturnRequestStatus.PENDING,
    val rejectionReason: String? = null,
    val createdDate: String = ""
)

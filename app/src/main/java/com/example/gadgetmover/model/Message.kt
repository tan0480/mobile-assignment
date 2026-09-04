package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

enum class MessageType { TEXT, IMAGE, LOCATION, PRODUCT, OFFER }

/**
 * One flat payload shape covering every [MessageType] — only the fields relevant to a message's
 * own type are populated. Stored as the `messages` table's `metadata` jsonb column; field names
 * have no `@SerialName` overrides, so the jsonb keys are these literal camelCase names.
 */
@Serializable
data class MessageMetadata(
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String? = null,
    val productId: String? = null,
    val productTitle: String? = null,
    val productImage: String? = null,
    val offerSalePrice: Double? = null,
    val offerRentalRate: Double? = null
)

@Serializable
data class Message(
    val id: String,
    val threadId: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val type: MessageType = MessageType.TEXT,
    val metadata: MessageMetadata? = null
)

@Serializable
data class ChatThread(
    val id: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String,
    val productId: String? = null,
    val productTitle: String? = null,
    val productImage: String? = null,
    val lastMessage: String,
    val lastMessageTime: String,
    val lastMessageType: MessageType = MessageType.TEXT,
    val unreadCount: Int = 0
)

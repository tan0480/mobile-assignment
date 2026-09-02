package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String,
    val reviewerName: String,
    val reviewerAvatar: String,
    val rating: Float,
    val comment: String,
    val date: String,
    val relatedProductTitle: String? = null
)

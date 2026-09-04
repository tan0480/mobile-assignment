package com.example.gadgetmover.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage

/** Displays the user's uploaded avatar, falling back to their initial only when unavailable. */
@Composable
fun UserAvatar(
    avatarUrl: String,
    displayName: String,
    modifier: Modifier = Modifier,
    fallbackBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
    fallbackContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String? = null
) {
    var imageFailed by remember(avatarUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fallbackBackground),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank() && !imageFailed) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { imageFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                displayName.trim().take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = fallbackContentColor
            )
        }
    }
}

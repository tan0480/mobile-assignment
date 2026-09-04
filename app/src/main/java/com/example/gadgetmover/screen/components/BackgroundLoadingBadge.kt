package com.example.gadgetmover.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.ui.theme.BrandOrange

/**
 * A small, self-contained loading chip for a silent page-entry refresh — unlike
 * [AppPullToRefreshBox]'s own spinner (which only appears for an actual user drag), this never
 * moves or resizes the content behind it. Carries its own background so it stays legible over
 * whatever content happens to be underneath.
 */
@Composable
fun BackgroundLoadingBadge(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .size(36.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandOrange, strokeWidth = 2.dp)
    }
}

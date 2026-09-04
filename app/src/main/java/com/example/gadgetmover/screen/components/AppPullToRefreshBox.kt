package com.example.gadgetmover.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.ui.theme.BrandOrange
import kotlin.math.roundToInt

/**
 * A pull-to-refresh container where the [content] itself is dragged downward as the user pulls
 * — revealing a fixed indicator area pinned behind it — rather than a spinner merely floating on
 * top of stationary content (Material 3's default `PullToRefreshBox` behavior). Matches the
 * whole-list-shifts-down feel of apps like Lazada/Shopee.
 */
@Composable
fun AppPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { PullToRefreshDefaults.PositionalThreshold.toPx() } }

    Box(modifier = modifier.pullToRefresh(state = state, isRefreshing = isRefreshing, onRefresh = onRefresh)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(PullToRefreshDefaults.PositionalThreshold),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isRefreshing || state.distanceFraction > 0f) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), color = BrandOrange, strokeWidth = 2.5.dp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (state.distanceFraction.coerceIn(0f, 1.4f) * thresholdPx).roundToInt()) },
            content = content
        )
    }
}

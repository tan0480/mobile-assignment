package com.example.gadgetmover.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.BrowseHistoryRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.ProductStatus
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.screen.components.BackgroundLoadingBadge
import com.example.gadgetmover.screen.components.ProductCard
import com.example.gadgetmover.ui.theme.AccentLime
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.ListingScoreCalculator
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onProductClick: (Product) -> Unit,
    onCategoryClick: (ProductCategory) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSeeAllCategories: () -> Unit,
    onLoginClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    // Not wrapped in `remember` — `ProductRepository.products` is a snapshot-backed list, so
    // reading it directly here (rather than caching a one-time snapshot) makes Home reactively
    // pick up whatever `refreshFromRemote()` below just pulled from Supabase, including listings
    // published from a different device.
    val user = AuthRepository.currentUser.value
    val savedProducts = ProductRepository.getSaved()
    val browsedProducts = BrowseHistoryRepository.recentProducts()
    val categoryInterest = (browsedProducts.map { it.category } + savedProducts.flatMap { listOf(it.category, it.category) })
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(3)
        .map { it.key }
        .toSet()
    val hasInterestHistory = browsedProducts.isNotEmpty() || savedProducts.isNotEmpty()
    val currentUserId = user?.id
    val recommended = ProductRepository.products
        .asSequence()
        .filterNot { it.sellerId == currentUserId || it.status == ProductStatus.SOLD }
        .sortedByDescending { product ->
            val interestScore = if (hasInterestHistory && product.category in categoryInterest) 40.0 else 0.0
            val freshnessScore = recommendationFreshnessScore(product.postedDate)
            val favoriteScore = if (ProductRepository.isSaved(product.id)) 15.0 else 0.0
            val baseScore = interestScore + freshnessScore + favoriteScore
            val completenessRatio = ListingScoreCalculator.calculateCompletenessRatio(product)
            baseScore * ListingScoreCalculator.boostMultiplier(completenessRatio)
        }
        .take(12)
        .toList()
    val isLoggedIn by AuthRepository.isLoggedIn
    val sessionRestored by AuthRepository.sessionRestored
    var isRefreshing by remember { mutableStateOf(false) }
    var isBackgroundLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isBackgroundLoading = true
        ProductRepository.refreshFromRemote()
        isBackgroundLoading = false
    }

    AppPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                ProductRepository.refreshFromRemote()
                isRefreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoggedIn && user != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle, // TODO: swap with custom ImageVector
                    contentDescription = "Guest",
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isLoggedIn && user != null) "Welcome back," else "Welcome to Gadget Mover",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isLoggedIn && user != null) user.name else "Browsing as guest",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search keyboards, audio, mice...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearchSubmit(query) }
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    SectionHeader(title = "Categories", actionLabel = "See all", onAction = onSeeAllCategories)
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    CategoryGrid(onCategoryClick = onCategoryClick)
                }

                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    SectionHeader(title = "Recommended for You")
                }
                items(recommended, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        isSaved = ProductRepository.isSaved(product.id),
                        onClick = { onProductClick(product) },
                        onSaveClick = { scope.launch { ProductRepository.toggleSaved(product.id) } }
                    )
                }
            }
        }
    }

        var bannerVisible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(10_000)
            bannerVisible = false
        }
        AnimatedVisibility(
            visible = bannerVisible && sessionRestored && !isLoggedIn,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            LoginReminderBanner(
                onLoginClick = onLoginClick,
                onDismiss = { bannerVisible = false }
            )
        }
        BackgroundLoadingBadge(visible = isBackgroundLoading, modifier = Modifier.align(Alignment.TopCenter))
    }
    }
}

private fun recommendationFreshnessScore(postedDate: String): Double {
    if (postedDate.equals("Just now", ignoreCase = true)) return 30.0
    val postedInstant = runCatching { OffsetDateTime.parse(postedDate).toInstant() }
        .recoverCatching { Instant.parse(postedDate) }
        .getOrNull() ?: return 0.0
    val ageDays = ChronoUnit.DAYS.between(postedInstant, Instant.now()).coerceAtLeast(0)
    return (30L - ageDays.coerceAtMost(30L)).toDouble()
}

@Composable
private fun LoginReminderBanner(onLoginClick: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandBlueDark),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Log in for the full experience",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    "Buy, rent, chat with sellers & save favorites",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            TextButton(onClick = onLoginClick) {
                Text("Log In", color = AccentLime, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close, // TODO: swap with custom ImageVector
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = BrandOrange,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

private data class CategoryItem(val category: ProductCategory, val icon: ImageVector)

private val categoryIcons = listOf(
    CategoryItem(ProductCategory.KEYBOARD, Icons.Filled.Keyboard),
    CategoryItem(ProductCategory.HEADPHONE, Icons.Filled.Headphones),
    CategoryItem(ProductCategory.AUDIO, Icons.Filled.SpeakerGroup),
    CategoryItem(ProductCategory.MOUSE, Icons.Filled.Mouse),
    CategoryItem(ProductCategory.LAPTOP, Icons.Filled.Laptop),
    CategoryItem(ProductCategory.MONITOR, Icons.Filled.Tv)
)

@Composable
private fun CategoryGrid(onCategoryClick: (ProductCategory) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(categoryIcons) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onCategoryClick(item.category) }
            ) {
                Column(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.category.label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Unspecified
                )
            }
        }
    }
}

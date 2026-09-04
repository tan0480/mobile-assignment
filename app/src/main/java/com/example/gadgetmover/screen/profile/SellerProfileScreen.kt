package com.example.gadgetmover.screen.profile

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.ProductStatus
import com.example.gadgetmover.model.User
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.screen.components.BackgroundLoadingBadge
import com.example.gadgetmover.screen.components.ProductCard
import com.example.gadgetmover.ui.theme.AccentLime
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.util.formatDisplayDateOnly
import kotlinx.coroutines.launch

@Composable
fun SellerProfileScreen(
    sellerId: String,
    sellerNameFallback: String,
    onBackClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onReviewsClick: () -> Unit = {}
) {
    var seller by remember(sellerId) { mutableStateOf<User?>(null) }
    var query by remember(sellerId) { mutableStateOf("") }
    var selectedCategory by remember(sellerId) { mutableStateOf<ProductCategory?>(null) }
    var statusFilter by remember(sellerId) { mutableStateOf<ProductStatus?>(null) }
    var showStatusSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isBackgroundLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sellerId) {
        isBackgroundLoading = true
        seller = AuthRepository.fetchProfile(sellerId)
        ProductRepository.refreshFromRemote()
        isBackgroundLoading = false
    }

    val allListings = ProductRepository.myListings(sellerId)
    val availableCategories = allListings.map { it.category }.distinct()
    val listings = allListings.filter { product ->
        (query.isBlank() || product.title.contains(query, ignoreCase = true)) &&
            (selectedCategory == null || product.category == selectedCategory) &&
            (statusFilter == null || product.status == statusFilter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SellerProfileTopBar(title = seller?.name ?: sellerNameFallback, onBackClick = onBackClick)

        AppPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    ProductRepository.refreshFromRemote()
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SellerHeaderCard(seller = seller, fallbackName = sellerNameFallback, onReviewsClick = onReviewsClick)

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "${allListings.size} listing${if (allListings.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search this seller's listings") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    BadgedBox(badge = {
                        if (statusFilter != null) Badge()
                    }) {
                        IconButton(
                            onClick = { showStatusSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = "Filter")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                CategoryPillRow(
                    categories = availableCategories,
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (listings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("No listings match", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(listings) { product ->
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
            BackgroundLoadingBadge(visible = isBackgroundLoading, modifier = Modifier.align(Alignment.TopCenter))
        }
    }

    if (showStatusSheet) {
        StatusFilterSheet(
            selected = statusFilter,
            onDismiss = { showStatusSheet = false },
            onSelect = {
                statusFilter = it
                showStatusSheet = false
            }
        )
    }
}

@Composable
private fun SellerProfileTopBar(title: String, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SellerHeaderCard(seller: User?, fallbackName: String, onReviewsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandBlueDark)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentLime),
                contentAlignment = Alignment.Center
            ) {
                if (!seller?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = seller.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        (seller?.name ?: fallbackName).take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        seller?.name ?: fallbackName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (seller?.isVerified == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = AccentLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (seller?.userId?.isNotBlank() == true) {
                    Text(
                        "@${seller.userId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentLime,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (seller != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Member since ${formatDisplayDateOnly(seller.joinedDate)} · ${seller.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onReviewsClick)
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = AccentLime,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${seller.rating} · ${seller.ratingCount} reviews",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentLime,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPillRow(categories: List<ProductCategory>, selected: ProductCategory?, onSelect: (ProductCategory?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PillChip(label = "All", selected = selected == null, onClick = { onSelect(null) })
        }
        items(categories) { category ->
            PillChip(label = category.label, selected = selected == category, onClick = { onSelect(category) })
        }
    }
}

@Composable
private fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterSheet(selected: ProductStatus?, onDismiss: () -> Unit, onSelect: (ProductStatus?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Text("Status", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            StatusOptionRow("All", selected == null) { onSelect(null) }
            StatusOptionRow("Available", selected == ProductStatus.AVAILABLE) { onSelect(ProductStatus.AVAILABLE) }
            StatusOptionRow("Sold", selected == ProductStatus.SOLD) { onSelect(ProductStatus.SOLD) }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatusOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

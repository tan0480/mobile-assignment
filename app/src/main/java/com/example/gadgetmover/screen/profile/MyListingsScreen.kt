package com.example.gadgetmover.screen.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductStatus
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.screen.components.BackgroundLoadingBadge
import com.example.gadgetmover.screen.components.ListingTypeBadge
import com.example.gadgetmover.ui.theme.SuccessGreen
import com.example.gadgetmover.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onBackClick: () -> Unit,
    onProductClick: (Product) -> Unit
) {
    val listings = ProductRepository.myListings(AuthRepository.currentUser.value?.id.orEmpty())
        .filter { it.status != ProductStatus.SOLD }
    var pendingDelete by remember { mutableStateOf<Product?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isBackgroundLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isBackgroundLoading = true
        ProductRepository.refreshFromRemote()
        isBackgroundLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Listings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: swap with custom ImageVector
                    }
                }
            )
        }
    ) { padding ->
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
                .padding(padding)
        ) {
        if (listings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Inventory2, // TODO: swap with custom ImageVector
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("You haven't listed anything yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listings, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onProductClick(product) },
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = product.images.firstOrNull(),
                                contentDescription = product.title,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ListingTypeBadge(listingType = product.listingType)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val showSalePrice = product.listingType == ListingType.BUY || product.listingType == ListingType.BOTH
                                val showRental = product.listingType == ListingType.RENT || product.listingType == ListingType.BOTH
                                Text(
                                    text = if (showSalePrice) formatMoney(product.price ?: 0.0) else "${formatMoney(product.rentalRatePerDay ?: 0.0)}/day",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (showRental && showSalePrice) {
                                    Text(
                                        text = "${formatMoney(product.rentalRatePerDay ?: 0.0)}/day",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                            IconButton(onClick = { pendingDelete = product }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove listing", tint = MaterialTheme.colorScheme.error) // TODO: swap with custom ImageVector
                            }
                        }
                    }
                }
            }
        }
        BackgroundLoadingBadge(visible = isBackgroundLoading, modifier = Modifier.align(Alignment.TopCenter))
        }
    }

    pendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove listing?") },
            text = { Text("\"${product.title}\" will be removed from the marketplace. This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = ProductRepository.removeProduct(product.id)
                            pendingDelete = null
                            if (!success) {
                                snackbarHostState.showSnackbar("Couldn't remove that listing. Please try again.")
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

package com.example.gadgetmover.screen.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductStatus
import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.displayText
import com.example.gadgetmover.model.filter.isFilled
import com.example.gadgetmover.screen.components.ConditionBadge
import com.example.gadgetmover.screen.components.FullScreenImageViewer
import com.example.gadgetmover.screen.components.ListingTypeBadge
import com.example.gadgetmover.screen.components.LoginRequiredDialog
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.formatMoney
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    product: Product,
    onBackClick: () -> Unit,
    onBuyNowClick: (Product) -> Unit,
    onRentClick: (Product) -> Unit,
    onMessageSellerClick: (Product) -> Unit,
    onEditClick: (Product) -> Unit,
    onLoginRequired: () -> Unit,
    onSellerClick: (Product) -> Unit = {}
) {
    var isSaved by remember { mutableStateOf(ProductRepository.isSaved(product.id)) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    val isOwner = product.sellerId == AuthRepository.currentUser.value?.id
    val scope = rememberCoroutineScope()

    fun requireLogin(action: () -> Unit) {
        if (AuthRepository.isLoggedIn.value) action() else showLoginDialog = true
    }

    Scaffold(
        bottomBar = {
            ProductDetailBottomBar(
                product = product,
                isOwner = isOwner,
                onBuyNowClick = { requireLogin { onBuyNowClick(product) } },
                onRentClick = { requireLogin { onRentClick(product) } },
                onMessageSellerClick = { requireLogin { onMessageSellerClick(product) } },
                onEditClick = { onEditClick(product) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box {
                AsyncImage(
                    model = product.images.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clickable(enabled = product.images.isNotEmpty()) {
                            previewImageUrl = product.images.first()
                        },
                    contentScale = ContentScale.Crop
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                ProductRepository.toggleSaved(product.id)
                                isSaved = ProductRepository.isSaved(product.id)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) BrandOrange else Color.Black
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ListingTypeBadge(listingType = product.listingType)
                    Spacer(modifier = Modifier.width(8.dp))
                    ConditionBadge(condition = product.condition)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(product.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                PriceSection(product)

                if (product.fulfillmentMethods.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        product.fulfillmentMethods.joinToString("  ·  ") { it.label },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                SellerCard(
                    product,
                    onMessageClick = { requireLogin { onMessageSellerClick(product) } },
                    onSellerClick = { onSellerClick(product) }
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                SpecsGrid(product)

                Spacer(modifier = Modifier.height(20.dp))
                Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(product.description, style = MaterialTheme.typography.bodyMedium)

                if (product.hasWarranty) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Warranty Included", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                product.warrantyDetails ?: "Contact seller for details",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showLoginDialog) {
        LoginRequiredDialog(onDismiss = { showLoginDialog = false }, onLoginClick = onLoginRequired)
    }

    previewImageUrl?.let { url ->
        FullScreenImageViewer(imageUrl = url, onDismiss = { previewImageUrl = null })
    }
}

@Composable
private fun PriceSection(product: Product) {
    when (product.listingType) {
        ListingType.BUY -> Text(
            formatMoney(product.price ?: 0.0),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        ListingType.RENT -> Text(
            "${formatMoney(product.rentalRatePerDay ?: 0.0)} / day",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        ListingType.BOTH -> Column {
            Text(
                "${formatMoney(product.price ?: 0.0)} to buy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "or ${formatMoney(product.rentalRatePerDay ?: 0.0)}/day to rent",
                style = MaterialTheme.typography.titleMedium,
                color = BrandOrange
            )
        }
    }
}

@Composable
private fun SellerCard(product: Product, onMessageClick: () -> Unit, onSellerClick: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSellerClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(product.sellerName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.sellerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "${product.sellerRating} · ${product.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onMessageClick) {
                Icon(Icons.Filled.Chat, contentDescription = "Message seller", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Driven entirely by whatever the seller actually filled in on the listing wizard — see [CategoryFilterRegistry]/`FilterFieldValueFormatting.kt`. All schema fields are shown; unfilled ones display "–". */
@Composable
private fun SpecsGrid(product: Product) {
    val schema = CategoryFilterRegistry.schemaFor(product.category)
    val entries = schema?.sections.orEmpty().map { field ->
        val value = product.specs.valueFor(field.key)
        val text = if (value != null && value.isFilled(field)) value.displayText(field, product.specs) else "–"
        field.label to text
    }
    if (entries.isEmpty()) {
        Text(
            "No additional specifications provided",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProductDetailBottomBar(
    product: Product,
    isOwner: Boolean,
    onBuyNowClick: () -> Unit,
    onRentClick: () -> Unit,
    onMessageSellerClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(shape = RoundedCornerShape(0.dp), elevation = CardDefaults.cardElevation(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isOwner) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Listing", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                OutlinedButton(onClick = onMessageSellerClick, modifier = Modifier.size(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.Chat, contentDescription = "Message")
                }
                if (product.status == ProductStatus.SOLD) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Sold Out", style = MaterialTheme.typography.titleMedium) }
                    return@Row
                }
                when (product.listingType) {
                    ListingType.BUY -> Button(
                        onClick = onBuyNowClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) { Text("Buy Now", style = MaterialTheme.typography.titleMedium) }

                    ListingType.RENT -> Button(
                        onClick = onRentClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) { Text("Rent Now", style = MaterialTheme.typography.titleMedium) }

                    ListingType.BOTH -> {
                        OutlinedButton(
                            onClick = onRentClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Rent") }
                        Button(
                            onClick = onBuyNowClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                        ) { Text("Buy Now") }
                    }
                }
            }
        }
    }
}

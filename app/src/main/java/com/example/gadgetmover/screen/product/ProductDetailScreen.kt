package com.example.gadgetmover.screen.product

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.model.FulfillmentMethod
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

/** Height of the hero product image — also the distance the TopAppBar takes to go from fully transparent to fully solid as the user scrolls. */
private val HeroHeight = 260.dp

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
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val heroHeightPx = with(LocalDensity.current) { HeroHeight.toPx() }
    val topBarProgress by remember { derivedStateOf { (scrollState.value / heroHeightPx).coerceIn(0f, 1f) } }

    fun requireLogin(action: () -> Unit) {
        if (AuthRepository.isLoggedIn.value) action() else showLoginDialog = true
    }

    fun toggleSaved() {
        scope.launch {
            ProductRepository.toggleSaved(product.id)
            isSaved = ProductRepository.isSaved(product.id)
        }
    }

    Scaffold(
        topBar = {
            ProductDetailTopBar(
                product = product,
                scrollProgress = topBarProgress,
                onBackClick = onBackClick,
                onShareClick = { shareProduct(context, product) }
            )
        },
        bottomBar = {
            ProductDetailBottomBar(
                product = product,
                isOwner = isOwner,
                isSaved = isSaved,
                onToggleSaved = { requireLogin { toggleSaved() } },
                onBuyNowClick = { requireLogin { onBuyNowClick(product) } },
                onRentClick = { requireLogin { onRentClick(product) } },
                onEditClick = { onEditClick(product) }
            )
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Neither inset is applied here — the hero image bleeds edge-to-edge behind the
                // transparent TopAppBar, and content scrolls edge-to-edge behind the transparent
                // bottom bar too, so the area around the floating Buy/Rent/favorite buttons shows
                // real page content instead of a flat Scaffold-background rectangle.
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeroHeight)
            ) {
                AsyncImage(
                    model = product.images.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = product.images.isNotEmpty()) {
                            previewImageUrl = product.images.first()
                        },
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ListingTypeBadge(listingType = product.listingType)
                    ConditionBadge(condition = product.condition)
                    if (FulfillmentMethod.SHIPPING in product.fulfillmentMethods) {
                        FulfillmentBadge(icon = Icons.Outlined.LocalShipping, label = "Delivery")
                    }
                    if (FulfillmentMethod.MEETUP in product.fulfillmentMethods) {
                        FulfillmentBadge(icon = Icons.Outlined.Place, label = "Meet-up")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(product.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                PriceSection(product)

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
                Spacer(modifier = Modifier.height(90.dp))
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

/** Plain-text price label shared by the compact TopAppBar title and the share message — [PriceSection] below renders the same data with per-listing-type styling instead. */
private fun productPriceLabel(product: Product): String = when (product.listingType) {
    ListingType.BUY -> formatMoney(product.price ?: 0.0)
    ListingType.RENT -> "${formatMoney(product.rentalRatePerDay ?: 0.0)} / day"
    ListingType.BOTH -> "${formatMoney(product.price ?: 0.0)} · ${formatMoney(product.rentalRatePerDay ?: 0.0)}/day"
}

private fun shareProduct(context: android.content.Context, product: Product) {
    val fulfillmentText = product.fulfillmentMethods.joinToString(", ") { it.label }
    val shareText = buildString {
        appendLine(product.title)
        appendLine("${product.condition.label} · ${productPriceLabel(product)}")
        if (fulfillmentText.isNotBlank()) appendLine(fulfillmentText)
        append("https://gadgetmover.app/product/${product.id}")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, product.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    try {
        context.startActivity(Intent.createChooser(sendIntent, "Share listing"))
    } catch (e: ActivityNotFoundException) {
        // No app can handle it — nothing further we can do.
    }
}

/**
 * Fully transparent at the top of [HeroHeight] worth of scroll, smoothly fading to a solid
 * [MaterialTheme.colorScheme.surface] background (with a divider) by the time the hero image has
 * scrolled off. The back and share buttons stay pinned throughout, gaining a translucent circular
 * badge while over the photo so they stay readable, and a compact thumbnail/price/title fade in
 * once the bar has gone solid.
 */
@Composable
private fun ProductDetailTopBar(
    product: Product,
    scrollProgress: Float,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = scrollProgress)
    val contentColor = lerp(Color.White, MaterialTheme.colorScheme.onSurface, scrollProgress)
    val badgeAlpha = (1f - scrollProgress) * 0.35f
    val compactInfoAlpha = ((scrollProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = badgeAlpha))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = contentColor)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .alpha(compactInfoAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = product.images.firstOrNull(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            productPriceLabel(product),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            product.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = badgeAlpha))
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = contentColor)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = scrollProgress))
    }
}

@Composable
private fun FulfillmentBadge(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
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

/**
 * Transparent floating container (no card/surface behind it) so it reads as a cluster of buttons
 * sitting over the screen rather than a solid dock. Owner-edit and sold-out logic is unchanged
 * from before; the only structural change for a live, non-owned listing is that the old
 * "Message Seller" text button is now the saved/favorite toggle (messaging the seller still works
 * from the chat icon on the seller card above), and Buy/Rent render as elevated floating buttons.
 */
@Composable
private fun ProductDetailBottomBar(
    product: Product,
    isOwner: Boolean,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    onBuyNowClick: () -> Unit,
    onRentClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A light rim on every floating control — needed now that the bar itself has no
        // card/background behind it, so each control has to read as its own shape against
        // whatever content happens to be scrolled underneath it.
        val rimBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
        val neutralBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

        if (isOwner) {
            Button(
                onClick = onEditClick,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                border = rimBorder
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Listing", style = MaterialTheme.typography.titleMedium)
            }
            return@Row
        }

        IconButton(
            onClick = onToggleSaved,
            modifier = Modifier
                .size(52.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(neutralBorder, RoundedCornerShape(14.dp))
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isSaved) "Remove from saved" else "Save",
                tint = if (isSaved) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (product.status == ProductStatus.SOLD) {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = neutralBorder
            ) { Text("Sold Out", style = MaterialTheme.typography.titleMedium) }
            return@Row
        }
        when (product.listingType) {
            ListingType.BUY -> ElevatedButton(
                onClick = onBuyNowClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = BrandOrange, contentColor = Color.White),
                border = rimBorder
            ) { Text("Buy Now", style = MaterialTheme.typography.titleMedium) }

            ListingType.RENT -> ElevatedButton(
                onClick = onRentClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = BrandOrange, contentColor = Color.White),
                border = rimBorder
            ) { Text("Rent Now", style = MaterialTheme.typography.titleMedium) }

            ListingType.BOTH -> {
                // Rent used to be a bare OutlinedButton (transparent container) — fine back when
                // a solid Card sat behind the whole bar, but invisible now that the bar itself is
                // transparent, so it needs a real fill of its own like Buy Now does.
                ElevatedButton(
                    onClick = onRentClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = rimBorder
                ) { Text("Rent") }
                ElevatedButton(
                    onClick = onBuyNowClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = BrandOrange, contentColor = Color.White),
                    border = rimBorder
                ) { Text("Buy Now") }
            }
        }
    }
}

package com.example.gadgetmover.screen.explore

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.gadgetmover.model.FilterState
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.SortOption
import com.example.gadgetmover.model.User
import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.CategoryFilterSchema
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.CommonFilterFields
import com.example.gadgetmover.model.filter.applyCategoryFilterState
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.screen.components.ProductCard
import com.example.gadgetmover.screen.explore.filter.CategoryPickerSheet
import com.example.gadgetmover.screen.explore.filter.DynamicFilterBottomSheet
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.util.formatMoney
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Round-trips FilterState/CategoryFilterState through JSON so rememberSaveable can restore the
// user's in-progress search/filters after navigating into Product Detail and back — this screen
// is fully disposed while that one's on top, so plain `remember` would otherwise reset them to
// whatever initialQuery/initialCategory/initialTransactionType Home last requested.
private val filterStateSaver = Saver<FilterState, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString(it) }
)
private val categoryFilterStateSaver = Saver<CategoryFilterState, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString(it) }
)

@Composable
fun ExploreScreen(
    initialQuery: String = "",
    initialCategory: ProductCategory? = null,
    initialTransactionType: ListingType? = null,
    onProductClick: (Product) -> Unit,
    onUserClick: (User) -> Unit = {}
) {
    var filterState by rememberSaveable(stateSaver = filterStateSaver) {
        mutableStateOf(
            FilterState(
                query = initialQuery,
                categories = if (initialCategory != null) setOf(initialCategory) else emptySet(),
                transactionType = initialTransactionType
            )
        )
    }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    val selectedCategory = filterState.categories.singleOrNull()
    var categoryFilterState by rememberSaveable(selectedCategory, stateSaver = categoryFilterStateSaver) {
        mutableStateOf(CategoryFilterState())
    }
    val combinedSchema = remember(selectedCategory) {
        CategoryFilterSchema(
            sections = CommonFilterFields.fields + (selectedCategory?.let { CategoryFilterRegistry.schemaFor(it) }?.sections ?: emptyList())
        )
    }
    // Not `remember`-memoized: `ProductRepository.search` reads the snapshot-backed `products`
    // list directly, so recomputing this on every recomposition (not just when filterState/
    // categoryFilterState change) is what lets a `refreshFromRemote()` completing while this
    // screen is open — e.g. a listing another device just published — actually show up here.
    val results = ProductRepository.search(filterState).applyCategoryFilterState(categoryFilterState, combinedSchema)

    var isRefreshing by remember { mutableStateOf(false) }
    var userResults by remember { mutableStateOf<List<User>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ProductRepository.refreshFromRemote()
    }

    // Debounced so every keystroke doesn't fire its own network query — only searches once
    // typing pauses for 300ms, and a cleared box clears results immediately with no query at all.
    LaunchedEffect(filterState.query) {
        if (filterState.query.isBlank()) {
            userResults = emptyList()
        } else {
            delay(300)
            userResults = AuthRepository.searchUsers(filterState.query)
        }
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
        modifier = Modifier.fillMaxSize()
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = filterState.query,
                onValueChange = { filterState = filterState.copy(query = it) },
                placeholder = { Text("Search items or user") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            val activeFilterCount = categoryFilterState.activeFieldCount(combinedSchema)
            BadgedBox(badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(activeFilterCount.toString()) }
                }
            }) {
                IconButton(
                    onClick = {
                        if (selectedCategory != null) showFilterSheet = true else showCategoryPicker = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filters") // TODO: swap with custom ImageVector
                }
            }
        }

        if (userResults.isNotEmpty()) {
            UserResultsRow(users = userResults, onUserClick = onUserClick)
        }

        CategoryPillRow(
            selected = selectedCategory,
            onSelect = { category ->
                filterState = filterState.copy(categories = if (category != null) setOf(category) else emptySet())
            }
        )

        TransactionTypeTabs(
            selected = filterState.transactionType,
            onSelect = { filterState = filterState.copy(transactionType = it) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                buildResultCountLabel(results.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SortMenuButton(
                    selected = filterState.sortBy,
                    onSelect = { filterState = filterState.copy(sortBy = it) }
                )
                ViewToggleButton(
                    icon = Icons.Filled.GridView, // TODO: swap with custom ImageVector
                    selected = isGridView,
                    onClick = { isGridView = true }
                )
                ViewToggleButton(
                    icon = Icons.Filled.ViewList, // TODO: swap with custom ImageVector
                    selected = !isGridView,
                    onClick = { isGridView = false }
                )
            }
        }

        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.SearchOff, // TODO: swap with custom ImageVector
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No gadgets match your filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isGridView) 2 else 1),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { product ->
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
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            onDismiss = { showCategoryPicker = false },
            onSelect = { category ->
                filterState = filterState.copy(categories = setOf(category))
                showCategoryPicker = false
                showFilterSheet = true
            }
        )
    }

    if (showFilterSheet && selectedCategory != null) {
        DynamicFilterBottomSheet(
            category = selectedCategory,
            filterState = categoryFilterState,
            onDismiss = { showFilterSheet = false },
            onApply = {
                categoryFilterState = it
                showFilterSheet = false
            },
            onReset = {
                categoryFilterState = CategoryFilterState()
                showFilterSheet = false
            }
        )
    }
}

/** Shown above the category pills whenever the search box matches a seller by name or @handle, alongside the product grid below — tapping a row goes straight to that seller's profile. */
@Composable
private fun UserResultsRow(users: List<User>, onUserClick: (User) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            "Sellers",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(users) { user ->
                UserResultCard(user = user, onClick = { onUserClick(user) })
            }
        }
    }
}

@Composable
private fun UserResultCard(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BrandBlueDark.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    user.name.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlueDark
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                user.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.userId.isNotBlank()) {
                Text(
                    "@${user.userId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CategoryPillRow(selected: ProductCategory?, onSelect: (ProductCategory?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PillChip(label = "All", selected = selected == null, onClick = { onSelect(null) })
        }
        items(ProductCategory.entries) { category ->
            PillChip(label = category.label, selected = selected == category, onClick = { onSelect(category) })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TransactionTypeTabs(selected: ListingType?, onSelect: (ListingType?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PillChip("All", selected == null) { onSelect(null) }
        PillChip("Buy", selected == ListingType.BUY) { onSelect(ListingType.BUY) }
        PillChip("Rent", selected == ListingType.RENT) { onSelect(ListingType.RENT) }
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

@Composable
private fun SortMenuButton(selected: SortOption, onSelect: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sort: ${selected.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ViewToggleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    // A plain clickable Box instead of IconButton: Material3's IconButton enforces a 48dp minimum
    // touch target regardless of the size() passed in, which was pushing this button's true layout
    // width past the 36dp visual box and crowding it into its neighbor.
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun buildResultCountLabel(count: Int): String = "$count items nearby"


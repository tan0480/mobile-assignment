package com.example.gadgetmover.screen.explore.filter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.model.LocationRadiusFilter
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.CommonFilterFields
import com.example.gadgetmover.model.filter.FilterField
import com.example.gadgetmover.model.filter.FilterFieldValue
import com.example.gadgetmover.model.filter.MalaysiaStates
import com.example.gadgetmover.model.filter.isVisible
import com.example.gadgetmover.ui.theme.BrandOrange
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Schema-driven replacement for the old hardcoded [com.example.gadgetmover.screen.explore.FilterBottomSheet]:
 * a left quick-nav rail lists every section (the common Price/Condition/Brand fields plus whatever
 * [CategoryFilterRegistry.schemaFor] returns for [category] — currently just Keyboards, other
 * categories fall back to common-only filters until their schemas are added) and the right pane
 * scrolls through each section in full; tapping a rail entry jumps the right pane there, and the
 * rail highlight follows whichever section is currently scrolled into view.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DynamicFilterScreen(
    category: ProductCategory,
    filterState: CategoryFilterState,
    onDismiss: () -> Unit,
    onApply: (ProductCategory, CategoryFilterState) -> Unit,
    onReset: (ProductCategory) -> Unit,
    locationFilter: LocationRadiusFilter? = null,
    onLocationFilterClick: () -> Unit = {}
) {
    var draftCategory by remember(category) { mutableStateOf(category) }
    var draft by remember(filterState) { mutableStateOf(filterState) }
    var showStatePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val categorySchema = remember(draftCategory) { CategoryFilterRegistry.schemaFor(draftCategory) }
    val baseFields: List<FilterField> = remember(categorySchema) {
        CommonFilterFields.fields + (categorySchema?.sections ?: emptyList())
    }
    // Fields with a `visibleWhen` dependency (e.g. a Bluetooth-only polling-rate field) drop in
    // and out as the user picks options, so this list — and the rail/pane it drives — is derived
    // from the live draft rather than computed once.
    val allFields: List<FilterField> = remember(baseFields, draft) {
        baseFields.filter { it.isVisible(draft) }
    }

    val railListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            totalItems > 0 &&
                (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >= totalItems - 1
        }
    }
    var activeSectionIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(draftCategory) {
        activeSectionIndex = 0
        listState.scrollToItem(0)
        railListState.scrollToItem(0)
    }

    // Keep the scrollspy out of composition-time layout feedback. The bottom rule wins so a short
    // final section is highlighted as soon as the end is reached; otherwise use the last section
    // whose top crossed the viewport anchor. distinctUntilChanged prevents sub-pixel boundary
    // corrections from repeatedly invalidating the rail.
    LaunchedEffect(listState, allFields.size) {
        snapshotFlow {
            if (allFields.isEmpty()) {
                0
            } else if (isAtBottom) {
                allFields.lastIndex
            } else {
                val layoutInfo = listState.layoutInfo
                val anchor = layoutInfo.viewportStartOffset + 8
                (layoutInfo.visibleItemsInfo.lastOrNull { it.offset <= anchor }?.index
                    ?: layoutInfo.visibleItemsInfo.firstOrNull()?.index
                    ?: 0).coerceIn(0, allFields.lastIndex)
            }
        }
            .distinctUntilChanged()
            .collect { activeSectionIndex = it }
    }

    // The rail scrolls independently of the content pane, so as the user scrolls the right side
    // the highlighted rail entry can drift out of view; bring it back on screen whenever it does.
    LaunchedEffect(activeSectionIndex) {
        val visibleIndices = railListState.layoutInfo.visibleItemsInfo.map { it.index }
        if (activeSectionIndex !in visibleIndices) {
            // The rail is independent from the right pane: following it cannot feed deltas back
            // into the user's gesture. Jump while the finger is down for immediate tracking, and
            // animate only for settled/programmatic changes.
            if (listState.isScrollInProgress) {
                railListState.scrollToItem(activeSectionIndex)
            } else {
                railListState.animateScrollToItem(activeSectionIndex)
            }
        }
    }

    if (showStatePicker) {
        val selectedState = (draft.valueFor(CommonFilterFields.sellerState.key) as? FilterFieldValue.SingleSelect)?.selectedId
        MalaysiaStatePickerDialog(
            selectedState = selectedState,
            onDismiss = { showStatePicker = false },
            onStateSelected = { state ->
                draft = draft.with(
                    CommonFilterFields.sellerState.key,
                    FilterFieldValue.SingleSelect(state)
                )
                showStatePicker = false
            }
        )
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            onDismiss = { showCategoryPicker = false },
            onSelect = { selectedCategory ->
                if (selectedCategory != draftCategory) {
                    draftCategory = selectedCategory
                    draft = CategoryFilterState()
                }
                showCategoryPicker = false
            }
        )
    }

    BackHandler(enabled = !showStatePicker && !showCategoryPicker && !showExitConfirmation) {
        showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Leave Filters?") },
            text = { Text("Your filter changes have not been applied. Discard them and go back?") },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Keep Editing")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Discard & Exit", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Filters", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { showExitConfirmation = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pagePadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(pagePadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showCategoryPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            draftCategory.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Change category")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onLocationFilterClick)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Browse Near Me",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (locationFilter != null) {
                                    "${locationFilter.address} · ${"%.1f".format(locationFilter.radiusKm)} km"
                                } else {
                                    "Not set"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Set browsing radius")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    state = railListState,
                    modifier = Modifier
                        .width(132.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    itemsIndexed(allFields) { index, field ->
                        val isSelected = index == activeSectionIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable {
                                    if (!listState.isScrollInProgress) {
                                        coroutineScope.launch { listState.animateScrollToItem(index) }
                                    }
                                }
                                .padding(vertical = 14.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .background(
                                        if (isSelected) BrandOrange else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                field.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CompositionLocalProvider(LocalOverscrollFactory provides null) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(allFields) { field ->
                            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                                if (field.key == CommonFilterFields.sellerState.key) {
                                    MalaysiaStateSelector(
                                        selectedState = (draft.valueFor(field.key) as? FilterFieldValue.SingleSelect)?.selectedId,
                                        onClick = { showStatePicker = true }
                                    )
                                } else {
                                    DynamicFilterField(
                                        field = field,
                                        value = draft.valueFor(field.key),
                                        state = draft,
                                        onValueChange = { newValue -> draft = draft.with(field.key, newValue) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { draft = draft.cleared(); onReset(draftCategory) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = { onApply(draftCategory, draft) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }
}

@Composable
private fun MalaysiaStateSelector(
    selectedState: String?,
    onClick: () -> Unit
) {
    Text(
        "State",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Malaysia State",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                selectedState?.stateDisplayName() ?: "All States / Malaysia",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = "Select state")
    }
}

@Composable
private fun MalaysiaStatePickerDialog(
    selectedState: String?,
    onDismiss: () -> Unit,
    onStateSelected: (String?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val choices = remember {
        listOf<String?>(null) + MalaysiaStates.ALL
    }
    val filteredChoices = remember(query) {
        choices.filter { state ->
            val label = state?.stateDisplayName() ?: "All States (Entire Malaysia)"
            query.isBlank() || label.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.heightIn(max = 560.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select State",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    placeholder = { Text("Search states") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(filteredChoices, key = { it ?: "all_states" }) { state ->
                        val label = state?.stateDisplayName() ?: "All States (Entire Malaysia)"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStateSelected(state) }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedState == state,
                                onClick = { onStateSelected(state) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun String.stateDisplayName(): String = when (this) {
    "WP Kuala Lumpur" -> "Wilayah Persekutuan Kuala Lumpur"
    "WP Labuan" -> "Wilayah Persekutuan Labuan"
    "WP Putrajaya" -> "Wilayah Persekutuan Putrajaya"
    else -> this
}

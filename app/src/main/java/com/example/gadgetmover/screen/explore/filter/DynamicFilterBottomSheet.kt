package com.example.gadgetmover.screen.explore.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.filter.CategoryFilterRegistry
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.model.filter.CommonFilterFields
import com.example.gadgetmover.model.filter.FilterField
import com.example.gadgetmover.model.filter.isVisible
import com.example.gadgetmover.ui.theme.BrandOrange
import kotlinx.coroutines.launch

/**
 * Schema-driven replacement for the old hardcoded [com.example.gadgetmover.screen.explore.FilterBottomSheet]:
 * a left quick-nav rail lists every section (the common Price/Condition/Brand fields plus whatever
 * [CategoryFilterRegistry.schemaFor] returns for [category] — currently just Keyboards, other
 * categories fall back to common-only filters until their schemas are added) and the right pane
 * scrolls through each section in full; tapping a rail entry jumps the right pane there, and the
 * rail highlight follows whichever section is currently scrolled into view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFilterBottomSheet(
    category: ProductCategory,
    filterState: CategoryFilterState,
    onDismiss: () -> Unit,
    onApply: (CategoryFilterState) -> Unit,
    onReset: () -> Unit
) {
    var draft by remember(filterState) { mutableStateOf(filterState) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categorySchema = remember(category) { CategoryFilterRegistry.schemaFor(category) }
    val baseFields: List<FilterField> = remember(categorySchema) {
        listOf(CommonFilterFields.price, CommonFilterFields.condition) +
            (categorySchema?.sections ?: emptyList())
    }
    // Fields with a `visibleWhen` dependency (e.g. a Bluetooth-only polling-rate field) drop in
    // and out as the user picks options, so this list — and the rail/pane it drives — is derived
    // from the live draft rather than computed once.
    val allFields: List<FilterField> = remember(baseFields, draft) {
        baseFields.filter { it.isVisible(draft) }
    }

    val listState = rememberLazyListState()
    val railListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // "Scrollspy": the active section is the last one whose top has crossed near the top of the
    // viewport — not simply the first visible item (that flips the instant a section's top sliver
    // peeks into view, before the user has actually scrolled to it) and not "whichever section
    // covers the most on-screen height" either (that loses to a tall neighbor immediately after
    // tapping a rail entry for a short section, e.g. tapping "Brand" landing it at the top but a
    // much taller "Layout & Form Factor" right below it winning on total visible area).
    val activeSectionIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val anchor = layoutInfo.viewportStartOffset + 8
            layoutInfo.visibleItemsInfo.lastOrNull { it.offset <= anchor }?.index
                ?: layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: 0
        }
    }

    // The rail scrolls independently of the content pane, so as the user scrolls the right side
    // the highlighted rail entry can drift out of view; bring it back on screen whenever it does.
    LaunchedEffect(activeSectionIndex) {
        val visibleIndices = railListState.layoutInfo.visibleItemsInfo.map { it.index }
        if (activeSectionIndex !in visibleIndices) {
            railListState.animateScrollToItem(activeSectionIndex)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    category.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth().height(440.dp)) {
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
                                    coroutineScope.launch { listState.animateScrollToItem(index) }
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

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    items(allFields) { field ->
                        Column(modifier = Modifier.padding(bottom = 20.dp)) {
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { draft = draft.cleared(); onReset() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }
}

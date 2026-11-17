package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.CalculationHistoryItem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassSubScreenTopBar
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.viewmodel.HistoryUiState
import com.example.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    viewModel: HistoryViewModel,
    onReuseItem: (CalculationHistoryItem) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    val copiedMsg = stringResource(R.string.msg_copied_to_clipboard)

    val groupedItems = remember(uiState.items) {
        groupHistoryByDay(uiState.items)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            LiquidGlassSubScreenTopBar(
                title = stringResource(R.string.title_history),
                onBack = onBack,
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.functionGlassCenter.copy(alpha = 0.55f))
                                .border(1.dp, palette.borderHighlight, CircleShape)
                                .testTag("history_clear_all_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.action_clear_all),
                                tint = ErrorCoral
                            )
                        }
                    }
                }
            )

            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.hint_search_history),
                        color = palette.textTertiary
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.textSecondary)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.cd_clear_search),
                                tint = palette.textSecondary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.accentPrimary,
                    unfocusedBorderColor = palette.borderSubtle,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("history_search_input")
            )

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurface)
                                .border(1.dp, palette.borderHighlight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = palette.accentGlow,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = if (uiState.searchQuery.isBlank()) {
                                stringResource(R.string.empty_history_title)
                            } else {
                                "No matching calculations"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.empty_history_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedItems.forEach { (sectionTitle, sectionItems) ->
                        item(key = "header_$sectionTitle") {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentGlow,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }
                        items(sectionItems, key = { it.id }) { item ->
                            HistoryGlassCard(
                                item = item,
                                onReuse = { onReuseItem(item) },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(item.result))
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(copiedMsg)
                                    }
                                },
                                onDelete = { viewModel.onDeleteItem(item.id) }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = if (palette.isDark) Color(0xFF232327) else Color.White,
            title = {
                Text(stringResource(R.string.confirm_clear_history_title), color = palette.textPrimary)
            },
            text = {
                Text(stringResource(R.string.confirm_clear_history_body), color = palette.textSecondary)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onClearAll()
                        showClearConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.action_clear_all), color = ErrorCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel), color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun HistoryGlassCard(
    item: CalculationHistoryItem,
    onReuse: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val timeLabel = remember(item.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    LiquidGlassCard(
        onClick = onReuse,
        elevated = false,
        cornerRadius = 22.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary
                    )
                    if (item.isScientific) {
                        Text(
                            text = "Scientific",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentGlow,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.accentPrimary.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_result),
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_item),
                            tint = palette.textTertiary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${item.expression} =",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.result,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                color = palette.textPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun groupHistoryByDay(
    items: List<CalculationHistoryItem>
): List<Pair<String, List<CalculationHistoryItem>>> {
    if (items.isEmpty()) return emptyList()

    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val itemCal = Calendar.getInstance()
    val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val grouped = linkedMapOf<String, MutableList<CalculationHistoryItem>>()
    for (item in items) {
        itemCal.timeInMillis = item.timestamp
        val label = when {
            isSameDay(itemCal, todayCal) -> "Today"
            isSameDay(itemCal, yesterdayCal) -> "Yesterday"
            else -> dateFmt.format(Date(item.timestamp))
        }
        grouped.getOrPut(label) { mutableListOf() }.add(item)
    }
    return grouped.entries.map { it.key to it.value }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

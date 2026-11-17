package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.engine.NumberFormatter
import com.example.domain.model.CurrencyRate
import com.example.ui.components.GlassKeyStyle
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassCircleButton
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.LiquidGlassSubScreenTopBar
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SuccessMint
import com.example.ui.viewmodel.CurrencyUiState
import com.example.ui.viewmodel.CurrencyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PickerTarget { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    uiState: CurrencyUiState,
    viewModel: CurrencyViewModel,
    onBack: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val snackbarHostState = remember { SnackbarHostState() }

    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var editingCurrency by remember { mutableStateOf<CurrencyRate?>(null) }
    var swapRotationTarget by remember { mutableFloatStateOf(0f) }

    val swapRotation by animateFloatAsState(
        targetValue = swapRotationTarget,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "swap_rot"
    )

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissStatusMessage()
    }

    val fromRate = uiState.rates.firstOrNull { it.code == uiState.fromCode }
    val toRate = uiState.rates.firstOrNull { it.code == uiState.toCode }
    val formattedDate = remember(uiState.lastUpdatedMillis) {
        if (uiState.lastUpdatedMillis <= 0L) "Preloaded offline"
        else SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(uiState.lastUpdatedMillis))
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
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LiquidGlassSubScreenTopBar(
                title = stringResource(R.string.title_currency_converter),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.onRefreshOnlineRates() },
                        enabled = !uiState.isRefreshingOnline,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(palette.functionGlassCenter.copy(alpha = 0.55f))
                            .border(1.dp, palette.borderHighlight, CircleShape)
                            .testTag("currency_refresh_btn")
                    ) {
                        if (uiState.isRefreshingOnline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = palette.accentGlow
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CloudSync,
                                contentDescription = stringResource(R.string.action_refresh_online),
                                tint = palette.textPrimary
                            )
                        }
                    }
                }
            )

            // Offline Reference Rate Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SuccessMint.copy(alpha = 0.14f))
                        .border(0.8.dp, SuccessMint.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OfflineBolt,
                        contentDescription = null,
                        tint = SuccessMint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.badge_offline_reference),
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessMint
                    )
                }

                Text(
                    text = stringResource(R.string.label_last_updated, formattedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conversion Pair Cards + Swap Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CurrencyAmountCard(
                        label = stringResource(R.string.label_from),
                        rate = fromRate,
                        fallbackCode = uiState.fromCode,
                        displayValue = uiState.formattedInput,
                        isPrimaryInput = true,
                        onSelectCurrency = { pickerTarget = PickerTarget.FROM },
                        onEditRate = { fromRate?.let { editingCurrency = it } },
                        testTagPrefix = "from"
                    )

                    CurrencyAmountCard(
                        label = stringResource(R.string.label_to),
                        rate = toRate,
                        fallbackCode = uiState.toCode,
                        displayValue = uiState.convertedFormatted,
                        isPrimaryInput = false,
                        onSelectCurrency = { pickerTarget = PickerTarget.TO },
                        onEditRate = { toRate?.let { editingCurrency = it } },
                        testTagPrefix = "to"
                    )
                }

                // Floating Glass Swap Button centered between cards
                LiquidGlassCircleButton(
                    onClick = {
                        swapRotationTarget += 180f
                        viewModel.onSwapCurrencies()
                    },
                    style = GlassKeyStyle.ACCENT_EQUALS,
                    accessibilityLabel = stringResource(R.string.cd_swap_currencies),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .testTag("currency_swap_btn")
                ) { contentColor ->
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.cd_swap_currencies),
                        tint = contentColor,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(swapRotation)
                    )
                }
            }

            // Unit rate + Edit & Save Conversion bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { toRate?.let { editingCurrency = it } }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = uiState.unitRateDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_rate),
                        tint = palette.accentGlow,
                        modifier = Modifier.size(15.dp)
                    )
                }

                LiquidGlassPillButton(
                    onClick = { viewModel.onSaveRecentConversion() },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("currency_save_recent")
                ) { color ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                            contentDescription = null,
                            tint = palette.accentGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelMedium,
                            color = color
                        )
                    }
                }
            }

            // Recent Conversions strip (if any)
            if (uiState.recentConversions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.recentConversions.take(6).forEach { recent ->
                        val amtText = NumberFormatter.formatCurrencyAmount(
                            recent.amount,
                            uiState.settings.numberFormatStyle
                        )
                        val resText = NumberFormatter.formatCurrencyAmount(
                            recent.convertedAmount,
                            uiState.settings.numberFormatStyle
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.cardSurface)
                                .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                                .clickable { viewModel.onApplyRecentConversion(recent) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$amtText ${recent.fromCode} → $resText ${recent.toCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.disclaimer_reference_rates),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Built-in Liquid Glass Numeric Keypad for instant offline entry
            CurrencyNumericKeypad(
                onDigit = { viewModel.onKeypadDigit(it) },
                onDecimal = { viewModel.onKeypadDecimal() },
                onBackspace = { viewModel.onKeypadBackspace() },
                onClear = { viewModel.onKeypadClear() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
        )
    }

    // Currency Picker Sheet (Search + Favorites + All Currencies + Edit Rate)
    if (pickerTarget != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                pickerTarget = null
                viewModel.onSearchQueryChanged("")
            },
            sheetState = sheetState,
            containerColor = if (palette.isDark) Color(0xFF1E1E21) else Color(0xFFF6F7FB),
            contentColor = palette.textPrimary
        ) {
            CurrencyPickerSheet(
                rates = uiState.filteredRates,
                searchQuery = uiState.searchQuery,
                selectedCode = if (pickerTarget == PickerTarget.FROM) uiState.fromCode else uiState.toCode,
                onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                onSelectCurrency = { code ->
                    if (pickerTarget == PickerTarget.FROM) {
                        viewModel.onSelectFromCurrency(code)
                    } else {
                        viewModel.onSelectToCurrency(code)
                    }
                    pickerTarget = null
                    viewModel.onSearchQueryChanged("")
                },
                onToggleFavorite = { rate ->
                    viewModel.onToggleFavorite(rate.code, rate.isFavorite)
                },
                onEditRate = { rate ->
                    editingCurrency = rate
                }
            )
        }
    }

    // Manual Exchange-Rate Editor Dialog
    editingCurrency?.let { target ->
        ManualRateEditorDialog(
            currency = target,
            onDismiss = { editingCurrency = null },
            onSaveRate = { newRate ->
                viewModel.onUpdateManualRate(target.code, newRate)
                editingCurrency = null
            },
            onResetDefault = {
                viewModel.onResetRateToDefault(target.code)
                editingCurrency = null
            }
        )
    }
}

@Composable
private fun CurrencyAmountCard(
    label: String,
    rate: CurrencyRate?,
    fallbackCode: String,
    displayValue: String,
    isPrimaryInput: Boolean,
    onSelectCurrency: () -> Unit,
    onEditRate: () -> Unit,
    testTagPrefix: String
) {
    val palette = LocalLiquidGlassPalette.current
    val code = rate?.code ?: fallbackCode
    val name = rate?.name ?: fallbackCode
    val symbol = rate?.symbol ?: ""

    LiquidGlassCard(
        elevated = isPrimaryInput,
        cornerRadius = 22.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Selector Pill
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSelectCurrency)
                    .padding(4.dp)
                    .testTag("${testTagPrefix}_currency_selector"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary
                    )
                    if (rate?.isManuallyEdited == true) {
                        Text(
                            text = "• Custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentGlow,
                            modifier = Modifier.clickable(onClick = onEditRate)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(palette.functionGlassCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rate?.regionBadge ?: code.take(2),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                    }
                    Text(
                        text = code,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Choose currency",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Numeric Value
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$symbol $displayValue".trim(),
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = if (isPrimaryInput) FontWeight.SemiBold else FontWeight.Light,
                    fontSize = when {
                        displayValue.length > 14 -> 20.sp
                        displayValue.length > 10 -> 24.sp
                        else -> 30.sp
                    },
                    color = if (isPrimaryInput) palette.textPrimary else palette.accentGlow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("${testTagPrefix}_currency_amount")
                )
            }
        }
    }
}

@Composable
private fun CurrencyNumericKeypad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = 10.dp
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        val rows = listOf(
            listOf("7", "8", "9", "AC"),
            listOf("4", "5", "6", "DEL"),
            listOf("1", "2", "3", "."),
            listOf("00", "0")
        )
        rows.take(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                row.forEach { key ->
                    val style = when (key) {
                        "AC", "DEL" -> GlassKeyStyle.FUNCTION
                        "." -> GlassKeyStyle.OPERATOR
                        else -> GlassKeyStyle.DIGIT
                    }
                    LiquidGlassCircleButton(
                        onClick = {
                            when (key) {
                                "AC" -> onClear()
                                "DEL" -> onBackspace()
                                "." -> onDecimal()
                                else -> onDigit(key)
                            }
                        },
                        style = style,
                        accessibilityLabel = key,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("curr_key_$key")
                    ) { color ->
                        if (key == "DEL") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                contentDescription = stringResource(R.string.cd_backspace),
                                tint = color,
                                modifier = Modifier.size(21.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = if (key == "AC") 18.sp else 22.sp,
                                color = color
                            )
                        }
                    }
                }
            }
        }
        // Bottom row: 00 | 0 (2-col wide glass pill for symmetry)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            LiquidGlassPillButton(
                onClick = { onDigit("00") },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("curr_key_00")
            ) { color ->
                Text("00", fontFamily = PlusJakartaSansFamily, fontSize = 20.sp, color = color)
            }
            LiquidGlassPillButton(
                onClick = { onDigit("0") },
                modifier = Modifier
                    .weight(2f)
                    .height(54.dp)
                    .testTag("curr_key_0")
            ) { color ->
                Text("0", fontFamily = PlusJakartaSansFamily, fontSize = 22.sp, color = color)
            }
            LiquidGlassPillButton(
                onClick = onClear,
                isActive = true,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("curr_key_clear")
            ) { color ->
                Text("Clear", fontFamily = PlusJakartaSansFamily, fontSize = 15.sp, color = color)
            }
        }
    }
}

@Composable
private fun CurrencyPickerSheet(
    rates: List<CurrencyRate>,
    searchQuery: String,
    selectedCode: String,
    onSearchChanged: (String) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onToggleFavorite: (CurrencyRate) -> Unit,
    onEditRate: (CurrencyRate) -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val favorites = rates.filter { it.isFavorite }
    val others = rates.filter { !it.isFavorite }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 580.dp)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Select Currency",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.hint_search_currency),
                    color = palette.textTertiary
                )
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.textSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            tint = palette.textSecondary
                        )
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accentPrimary,
                unfocusedBorderColor = palette.borderSubtle,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .testTag("currency_search_input")
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_favorites),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentGlow,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(favorites, key = { "fav_${it.code}" }) { rate ->
                    CurrencyPickerRow(
                        rate = rate,
                        isSelected = rate.code == selectedCode,
                        onSelect = { onSelectCurrency(rate.code) },
                        onToggleFavorite = { onToggleFavorite(rate) },
                        onEditRate = { onEditRate(rate) }
                    )
                }
            }

            if (others.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_all_currencies),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(others, key = { "all_${it.code}" }) { rate ->
                    CurrencyPickerRow(
                        rate = rate,
                        isSelected = rate.code == selectedCode,
                        onSelect = { onSelectCurrency(rate.code) },
                        onToggleFavorite = { onToggleFavorite(rate) },
                        onEditRate = { onEditRate(rate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyPickerRow(
    rate: CurrencyRate,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditRate: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    LiquidGlassCard(
        onClick = onSelect,
        elevated = isSelected,
        cornerRadius = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(1.2.dp, palette.accentPrimary, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.functionGlassCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rate.regionBadge,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${rate.code} · ${rate.symbol}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        if (rate.isManuallyEdited) {
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentGlow
                            )
                        }
                    }
                    Text(
                        text = "${rate.name} (1 USD = ${rate.ratePerUsd})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (rate.code != "USD") {
                    IconButton(onClick = onEditRate, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.cd_edit_rate),
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (rate.isFavorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(R.string.cd_favorite_currency),
                        tint = if (rate.isFavorite) palette.accentGlow else palette.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualRateEditorDialog(
    currency: CurrencyRate,
    onDismiss: () -> Unit,
    onSaveRate: (Double) -> Unit,
    onResetDefault: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    var rateText by remember(currency.code) { mutableStateOf(currency.ratePerUsd.toString()) }
    val parsed = rateText.toDoubleOrNull()
    val isValid = parsed != null && parsed > 0.0 && currency.code != "USD"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isDark) Color(0xFF232327) else Color.White,
        title = {
            Text(
                text = "${stringResource(R.string.title_edit_rate)} (${currency.code})",
                color = palette.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currency.code == "USD") {
                    Text(
                        text = "USD is the base reference currency (1.0). Select another currency to edit its rate relative to 1 USD.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                } else {
                    Text(
                        text = "Set custom offline rate for 1 USD in ${currency.name} (${currency.code}):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                rateText = input
                            }
                        },
                        singleLine = true,
                        label = { Text("1 USD = ? ${currency.code}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_rate_input")
                    )
                }
            }
        },
        confirmButton = {
            if (currency.code != "USD") {
                TextButton(
                    onClick = { parsed?.let { onSaveRate(it) } },
                    enabled = isValid
                ) {
                    Text(stringResource(R.string.action_save), color = palette.accentGlow)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("OK", color = palette.accentGlow)
                }
            }
        },
        dismissButton = {
            Row {
                if (currency.isManuallyEdited) {
                    TextButton(onClick = onResetDefault) {
                        Text(stringResource(R.string.action_reset), color = palette.textSecondary)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = palette.textSecondary)
                }
            }
        }
    )
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AngleMode
import com.example.domain.model.CalculationHistoryItem
import com.example.ui.components.GlassKeyStyle
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassCircleButton
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.viewmodel.CalculatorUiState
import com.example.ui.viewmodel.CalculatorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel,
    onNavigateToCurrency: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }
    var showDisplayActions by remember { mutableStateOf(false) }

    val copiedMsg = stringResource(R.string.msg_copied_to_clipboard)
    val nothingToPasteMsg = stringResource(R.string.msg_nothing_to_paste)

    fun copyResultToClipboard() {
        val text = viewModel.getCopyableResult()
        if (text.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(text))
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(copiedMsg)
            }
        }
    }

    fun pasteFromClipboard() {
        val clip = clipboardManager.getText()?.text.orEmpty()
        if (clip.isEmpty() || !viewModel.onPasteText(clip)) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(nothingToPasteMsg)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .drawBehind {
                // Subtle ColorOS 16 ambient depth glow behind glass surfaces
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentPrimary.copy(alpha = if (palette.isDark) 0.055f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.88f),
                        radius = size.width * 0.72f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Compact Top Navigation Bar (4 thin white icons)
            CalculatorTopNavBar(
                isExpanded = uiState.isDisplayExpanded,
                isScientific = uiState.settings.isScientificExpanded,
                showOverflowMenu = showOverflowMenu,
                onToggleExpand = { viewModel.onToggleDisplayExpanded() },
                onToggleScientific = { viewModel.onToggleScientificMode() },
                onOpenTools = {
                    viewModel.onModeSwitchHaptic()
                    showToolsSheet = true
                },
                onOpenMenu = { showOverflowMenu = true },
                onDismissMenu = { showOverflowMenu = false },
                onNavigateHistory = {
                    showOverflowMenu = false
                    viewModel.onModeSwitchHaptic()
                    onNavigateToHistory()
                },
                onNavigateCurrency = {
                    showOverflowMenu = false
                    viewModel.onModeSwitchHaptic()
                    onNavigateToCurrency()
                },
                onCopyResult = {
                    showOverflowMenu = false
                    copyResultToClipboard()
                },
                onPaste = {
                    showOverflowMenu = false
                    pasteFromClipboard()
                },
                onNavigateSettings = {
                    showOverflowMenu = false
                    viewModel.onModeSwitchHaptic()
                    onNavigateToSettings()
                }
            )

            // 2. Responsive Layout Body (Portrait single-column vs Wide side-by-side)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWideScreen = maxWidth >= 660.dp
                val keypadMaxWidth = if (isWideScreen) 400.dp else 460.dp

                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            CalculatorDisplayArea(
                                uiState = uiState,
                                showDisplayActions = showDisplayActions,
                                onToggleDisplayActions = { showDisplayActions = !showDisplayActions },
                                onCopy = {
                                    copyResultToClipboard()
                                    showDisplayActions = false
                                },
                                onPaste = {
                                    pasteFromClipboard()
                                    showDisplayActions = false
                                },
                                onReuseHistory = { viewModel.onReuseHistoryItem(it) },
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedVisibility(
                                visible = uiState.settings.isScientificExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                ScientificPadGrid(
                                    angleMode = uiState.settings.angleMode,
                                    onToggleAngleMode = { viewModel.onToggleAngleMode() },
                                    onScientificInput = { viewModel.onScientificInput(it) },
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }

                        StandardCircularKeypad(
                            viewModel = viewModel,
                            maxWidth = keypadMaxWidth,
                            modifier = Modifier
                                .widthIn(max = keypadMaxWidth)
                                .fillMaxHeight()
                                .padding(bottom = 12.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Generous display space matching ColorOS 16 proportions
                        CalculatorDisplayArea(
                            uiState = uiState,
                            showDisplayActions = showDisplayActions,
                            onToggleDisplayActions = { showDisplayActions = !showDisplayActions },
                            onCopy = {
                                copyResultToClipboard()
                                showDisplayActions = false
                            },
                            onPaste = {
                                pasteFromClipboard()
                                showDisplayActions = false
                            },
                            onReuseHistory = { viewModel.onReuseHistoryItem(it) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        // Scientific drawer (smoothly expands above 4x5 keypad)
                        AnimatedVisibility(
                            visible = uiState.settings.isScientificExpanded,
                            enter = expandVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(tween(180)),
                            exit = shrinkVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ) + fadeOut(tween(140))
                        ) {
                            ScientificPadGrid(
                                angleMode = uiState.settings.angleMode,
                                onToggleAngleMode = { viewModel.onToggleAngleMode() },
                                onScientificInput = { viewModel.onScientificInput(it) },
                                modifier = Modifier
                                    .widthIn(max = keypadMaxWidth)
                                    .padding(bottom = 10.dp)
                            )
                        }

                        // Exact 4-column x 5-row circular keypad
                        val keypadWeight = if (uiState.settings.isScientificExpanded) 1.35f else 1.48f
                        StandardCircularKeypad(
                            viewModel = viewModel,
                            maxWidth = keypadMaxWidth,
                            modifier = Modifier
                                .weight(keypadWeight)
                                .widthIn(max = keypadMaxWidth)
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp, start = 20.dp, end = 20.dp)
        )
    }

    if (showToolsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showToolsSheet = false },
            sheetState = sheetState,
            containerColor = if (palette.isDark) Color(0xFF1F1F22) else Color(0xFFF7F8FC),
            contentColor = palette.textPrimary
        ) {
            CalculatorToolsSheetContent(
                isScientific = uiState.settings.isScientificExpanded,
                onSelectStandard = {
                    if (uiState.settings.isScientificExpanded) {
                        viewModel.onToggleScientificMode()
                    }
                    showToolsSheet = false
                },
                onSelectScientific = {
                    if (!uiState.settings.isScientificExpanded) {
                        viewModel.onToggleScientificMode()
                    }
                    showToolsSheet = false
                },
                onOpenCurrency = {
                    showToolsSheet = false
                    viewModel.onModeSwitchHaptic()
                    onNavigateToCurrency()
                },
                onOpenHistory = {
                    showToolsSheet = false
                    viewModel.onModeSwitchHaptic()
                    onNavigateToHistory()
                },
                onOpenSettings = {
                    showToolsSheet = false
                    viewModel.onModeSwitchHaptic()
                    onNavigateToSettings()
                }
            )
        }
    }
}

@Composable
private fun CalculatorTopNavBar(
    isExpanded: Boolean,
    isScientific: Boolean,
    showOverflowMenu: Boolean,
    onToggleExpand: () -> Unit,
    onToggleScientific: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateCurrency: () -> Unit,
    onCopyResult: () -> Unit,
    onPaste: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Expand / Collapse calculator icon
        IconButton(
            onClick = onToggleExpand,
            modifier = Modifier
                .size(48.dp)
                .testTag("nav_expand")
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.CloseFullscreen else Icons.Outlined.OpenInFull,
                contentDescription = stringResource(R.string.cd_expand_collapse),
                tint = if (isExpanded) palette.accentGlow else palette.textPrimary.copy(alpha = 0.88f),
                modifier = Modifier.size(21.dp)
            )
        }

        // Right cluster: 2. Scientific, 3. Tools/Mode, 4. Three-dot overflow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onToggleScientific,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_scientific")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Functions,
                    contentDescription = stringResource(R.string.cd_scientific_mode),
                    tint = if (isScientific) palette.accentGlow else palette.textPrimary.copy(alpha = 0.88f),
                    modifier = Modifier.size(23.dp)
                )
            }

            IconButton(
                onClick = onOpenTools,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_tools")
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = stringResource(R.string.cd_tools_mode),
                    tint = palette.textPrimary.copy(alpha = 0.88f),
                    modifier = Modifier.size(21.dp)
                )
            }

            Box {
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("nav_menu")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = palette.textPrimary.copy(alpha = 0.88f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = onDismissMenu,
                    containerColor = if (palette.isDark) Color(0xFF252529) else Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_history), color = palette.textPrimary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.History, contentDescription = null, tint = palette.textPrimary)
                        },
                        onClick = onNavigateHistory
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_currency_converter), color = palette.textPrimary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.CurrencyExchange, contentDescription = null, tint = palette.accentGlow)
                        },
                        onClick = onNavigateCurrency
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_copy_result), color = palette.textPrimary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = palette.textPrimary)
                        },
                        onClick = onCopyResult
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_paste_number), color = palette.textPrimary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = palette.textPrimary)
                        },
                        onClick = onPaste
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_settings), color = palette.textPrimary) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Settings, contentDescription = null, tint = palette.textPrimary)
                        },
                        onClick = onNavigateSettings
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorDisplayArea(
    uiState: CalculatorUiState,
    showDisplayActions: Boolean,
    onToggleDisplayActions: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onReuseHistory: (CalculationHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalLiquidGlassPalette.current
    val exprScroll = rememberScrollState()

    LaunchedEffect(uiState.displayExpression) {
        exprScroll.animateScrollTo(exprScroll.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { if (showDisplayActions) onToggleDisplayActions() },
                onLongClick = onToggleDisplayActions
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // Inline quick-peek history when Expand icon is toggled
        AnimatedVisibility(
            visible = uiState.isDisplayExpanded && uiState.recentHistory.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                uiState.recentHistory.take(3).forEach { item ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.functionGlassCenter.copy(alpha = 0.45f))
                            .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                            .clickable { onReuseHistory(item) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.expression} =",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.result,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accentGlow
                        )
                    }
                }
            }
        }

        // Quick Copy / Paste floating Liquid Glass pill on long-press
        AnimatedVisibility(
            visible = showDisplayActions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.cardSurfaceElevated)
                    .border(1.dp, palette.borderHighlight, RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onCopy)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onPaste)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        tint = palette.accentGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Paste",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentGlow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Smaller previous expression line
        AnimatedVisibility(
            visible = uiState.previousExpression.isNotEmpty(),
            enter = fadeIn(tween(180)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(120))
        ) {
            Text(
                text = uiState.previousExpression,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        // Main large figure (either active expression or final evaluated result)
        val primaryText = if (uiState.justEvaluated && uiState.previewOrResult.isNotEmpty()) {
            uiState.previewOrResult
        } else {
            uiState.displayExpression
        }

        val targetPrimarySp = computeScaledFontSize(
            length = primaryText.length,
            isCompact = uiState.settings.isScientificExpanded
        )
        val animatedPrimarySp by animateFloatAsState(
            targetValue = targetPrimarySp.value,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "primary_font_scale"
        )

        val primaryColor = if (uiState.errorMessage != null) ErrorCoral else palette.textPrimary

        AnimatedContent(
            targetState = primaryText to uiState.justEvaluated,
            transitionSpec = {
                (fadeIn(tween(140)) + slideInVertically { if (targetState.second) it / 4 else 0 }) togetherWith
                    (fadeOut(tween(90)) + slideOutVertically { if (targetState.second) -it / 6 else 0 })
            },
            label = "display_main_anim"
        ) { (textValue, _) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(exprScroll),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = textValue,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = animatedPrimarySp.sp,
                        lineHeight = (animatedPrimarySp * 1.12f).sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = primaryColor,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("calc_display_main")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Live preview line while typing (hidden after '=' since primary text shows the result)
        val showPreviewLine = !uiState.justEvaluated && uiState.previewOrResult.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showPreviewLine,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(100))
            ) {
                val previewColor = if (uiState.previewOrResult == "Cannot divide by 0") {
                    ErrorCoral
                } else {
                    palette.textSecondary
                }
                Text(
                    text = if (uiState.previewOrResult == "Cannot divide by 0") {
                        uiState.previewOrResult
                    } else {
                        "= ${uiState.previewOrResult}"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        fontFeatureSettings = "tnum"
                    ),
                    color = previewColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("calc_display_preview")
                )
            }
        }
    }
}

private fun computeScaledFontSize(length: Int, isCompact: Boolean): TextUnit {
    val maxSp = if (isCompact) 46f else 58f
    return when {
        length <= 7 -> maxSp.sp
        length <= 10 -> (maxSp - 8f).sp
        length <= 14 -> (maxSp - 16f).sp
        length <= 19 -> (maxSp - 22f).sp
        else -> 28.sp
    }
}

@Composable
private fun ScientificPadGrid(
    angleMode: AngleMode,
    onToggleAngleMode: () -> Unit,
    onScientificInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalLiquidGlassPalette.current
    val rows: List<List<Pair<String, String>>> = listOf(
        listOf(
            angleMode.name to "ANGLE_MODE",
            "(" to "(",
            ")" to ")",
            "π" to "π",
            "e" to "e"
        ),
        listOf(
            "sin" to "sin",
            "cos" to "cos",
            "tan" to "tan",
            "log" to "log",
            "ln" to "ln"
        ),
        listOf(
            "sin⁻¹" to "sin⁻¹",
            "cos⁻¹" to "cos⁻¹",
            "tan⁻¹" to "tan⁻¹",
            "√" to "√",
            "1/x" to "1/x"
        ),
        listOf(
            "x²" to "x²",
            "x³" to "x³",
            "xʸ" to "xʸ",
            "x!" to "x!",
            "|x|" to "abs"
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, action) ->
                    val isAngle = action == "ANGLE_MODE"
                    LiquidGlassPillButton(
                        onClick = {
                            if (isAngle) onToggleAngleMode() else onScientificInput(action)
                        },
                        isActive = isAngle,
                        accessibilityLabel = if (isAngle) "Toggle angle mode, currently ${angleMode.name}" else label,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("sci_${action}")
                    ) { contentColor ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = if (label.length > 3) 13.sp else 15.sp,
                                fontWeight = if (isAngle) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isAngle) palette.accentGlow else contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact 4-column x 5-row circular Liquid Glass keypad matching the ColorOS 16 layout:
 * Row 1: AC | % | Backspace | ÷
 * Row 2: 7  | 8 | 9         | ×
 * Row 3: 4  | 5 | 6         | −
 * Row 4: 1  | 2 | 3         | +
 * Row 5: 00 | 0 | .         | =
 */
@Composable
private fun StandardCircularKeypad(
    viewModel: CalculatorViewModel,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        val gap = if (this.maxHeight < 360.dp) 10.dp else 13.dp
        val availWidth = min(this.maxWidth, maxWidth)
        val diameterFromWidth = (availWidth - gap * 3) / 4f
        val diameterFromHeight = (this.maxHeight - gap * 4) / 5f
        val buttonSize = min(diameterFromWidth, diameterFromHeight).coerceAtLeast(48.dp)

        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: AC | % | Backspace | ÷
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CalcTextCircleKey(
                    label = "AC",
                    style = GlassKeyStyle.FUNCTION,
                    size = buttonSize,
                    fontSize = 22.sp,
                    testTag = "btn_ac",
                    onClick = { viewModel.onClearAll() }
                )
                CalcTextCircleKey(
                    label = "%",
                    style = GlassKeyStyle.FUNCTION,
                    size = buttonSize,
                    fontSize = 24.sp,
                    testTag = "btn_percent",
                    onClick = { viewModel.onPercent() }
                )
                LiquidGlassCircleButton(
                    onClick = { viewModel.onBackspace() },
                    onLongPressRepeat = { viewModel.onBackspace() },
                    style = GlassKeyStyle.FUNCTION,
                    accessibilityLabel = stringResource(R.string.cd_backspace),
                    modifier = Modifier
                        .size(buttonSize)
                        .testTag("btn_backspace")
                ) { contentColor ->
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = stringResource(R.string.cd_backspace),
                        tint = contentColor,
                        modifier = Modifier.size((buttonSize.value * 0.33f).dp.coerceIn(20.dp, 28.dp))
                    )
                }
                CalcTextCircleKey(
                    label = "÷",
                    style = GlassKeyStyle.OPERATOR,
                    size = buttonSize,
                    fontSize = 31.sp,
                    testTag = "btn_divide",
                    onClick = { viewModel.onOperator("÷") }
                )
            }

            // Row 2: 7 | 8 | 9 | ×
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CalcTextCircleKey("7", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_7") { viewModel.onDigit("7") }
                CalcTextCircleKey("8", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_8") { viewModel.onDigit("8") }
                CalcTextCircleKey("9", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_9") { viewModel.onDigit("9") }
                CalcTextCircleKey("×", GlassKeyStyle.OPERATOR, buttonSize, 30.sp, "btn_multiply") { viewModel.onOperator("×") }
            }

            // Row 3: 4 | 5 | 6 | −
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CalcTextCircleKey("4", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_4") { viewModel.onDigit("4") }
                CalcTextCircleKey("5", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_5") { viewModel.onDigit("5") }
                CalcTextCircleKey("6", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_6") { viewModel.onDigit("6") }
                CalcTextCircleKey("−", GlassKeyStyle.OPERATOR, buttonSize, 31.sp, "btn_minus") { viewModel.onOperator("−") }
            }

            // Row 4: 1 | 2 | 3 | +
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CalcTextCircleKey("1", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_1") { viewModel.onDigit("1") }
                CalcTextCircleKey("2", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_2") { viewModel.onDigit("2") }
                CalcTextCircleKey("3", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_3") { viewModel.onDigit("3") }
                CalcTextCircleKey("+", GlassKeyStyle.OPERATOR, buttonSize, 31.sp, "btn_plus") { viewModel.onOperator("+") }
            }

            // Row 5: 00 | 0 | . | =
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CalcTextCircleKey("00", GlassKeyStyle.DIGIT, buttonSize, 24.sp, "btn_00") { viewModel.onDigit("00") }
                CalcTextCircleKey("0", GlassKeyStyle.DIGIT, buttonSize, 28.sp, "btn_0") { viewModel.onDigit("0") }
                CalcTextCircleKey(".", GlassKeyStyle.DIGIT, buttonSize, 30.sp, "btn_dot") { viewModel.onDecimal() }
                CalcTextCircleKey("=", GlassKeyStyle.ACCENT_EQUALS, buttonSize, 32.sp, "btn_equals") { viewModel.onEquals() }
            }
        }
    }
}

@Composable
private fun CalcTextCircleKey(
    label: String,
    style: GlassKeyStyle,
    size: Dp,
    fontSize: TextUnit,
    testTag: String,
    onClick: () -> Unit
) {
    val scaledFont = if (size < 64.dp) (fontSize.value * 0.82f).sp else fontSize
    LiquidGlassCircleButton(
        onClick = onClick,
        style = style,
        accessibilityLabel = label,
        modifier = Modifier
            .size(size)
            .testTag(testTag)
    ) { contentColor ->
        Text(
            text = label,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = if (style == GlassKeyStyle.OPERATOR || style == GlassKeyStyle.ACCENT_EQUALS) {
                FontWeight.Normal
            } else {
                FontWeight.Normal
            },
            fontSize = scaledFont,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun CalculatorToolsSheetContent(
    isScientific: Boolean,
    onSelectStandard: () -> Unit,
    onSelectScientific: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Calculator Tools",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToolSheetTile(
                title = "Standard",
                subtitle = "Classic 4×5 glass pad",
                icon = Icons.Outlined.GridView,
                selected = !isScientific,
                onClick = onSelectStandard,
                modifier = Modifier.weight(1f)
            )
            ToolSheetTile(
                title = "Scientific",
                subtitle = "Trig, log, power & roots",
                icon = Icons.Outlined.Functions,
                selected = isScientific,
                onClick = onSelectScientific,
                modifier = Modifier.weight(1f)
            )
        }

        LiquidGlassCard(
            onClick = onOpenCurrency,
            elevated = true,
            cornerRadius = 22.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CurrencyExchange,
                            contentDescription = null,
                            tint = palette.accentGlow
                        )
                    }
                    Column {
                        Text(
                            text = "Offline Currency Converter",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "20 currencies with local reference rates & manual editor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToolSheetTile(
                title = "History",
                subtitle = "Reuse past results",
                icon = Icons.Outlined.History,
                selected = false,
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f)
            )
            ToolSheetTile(
                title = "Settings",
                subtitle = "Haptics, theme & format",
                icon = Icons.Outlined.Settings,
                selected = false,
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun ToolSheetTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalLiquidGlassPalette.current
    LiquidGlassCard(
        onClick = onClick,
        elevated = selected,
        cornerRadius = 22.dp,
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.4.dp,
                        color = palette.accentPrimary.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(22.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) palette.accentGlow else palette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 2
            )
        }
    }
}

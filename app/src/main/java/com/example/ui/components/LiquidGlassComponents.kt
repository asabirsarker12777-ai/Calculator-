package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalLiquidGlassPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GlassKeyStyle {
    DIGIT,
    FUNCTION,
    OPERATOR,
    ACCENT_EQUALS
}

/**
 * Circular Liquid Glass keypad button inspired by ColorOS 16:
 * - Soft ambient drop shadow
 * - Subtle radial glass gradient with dynamic highlight shift on press
 * - Directional specular rim border and delicate upper-arc reflection
 * - Smooth spring scale-down animation on touch
 * - Optional continuous repeat on long press (used for Backspace)
 */
@Composable
fun LiquidGlassCircleButton(
    onClick: () -> Unit,
    style: GlassKeyStyle,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    onLongPressRepeat: (() -> Unit)? = null,
    onLongClickOnce: (() -> Unit)? = null,
    content: @Composable BoxScope.(contentColor: Color) -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnRepeat by rememberUpdatedState(onLongPressRepeat)
    val currentOnLongOnce by rememberUpdatedState(onLongClickOnce)

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_btn_scale"
    )

    val sheenBoost by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_sheen"
    )

    val centerColor = when (style) {
        GlassKeyStyle.DIGIT -> palette.digitGlassCenter
        GlassKeyStyle.FUNCTION -> palette.functionGlassCenter
        GlassKeyStyle.OPERATOR -> palette.operatorGlassCenter
        GlassKeyStyle.ACCENT_EQUALS -> palette.accentGlow
    }

    val edgeColor = when (style) {
        GlassKeyStyle.DIGIT -> palette.digitGlassEdge
        GlassKeyStyle.FUNCTION -> palette.functionGlassEdge
        GlassKeyStyle.OPERATOR -> palette.operatorGlassEdge
        GlassKeyStyle.ACCENT_EQUALS -> palette.accentSecondary
    }

    val contentColor = when (style) {
        GlassKeyStyle.ACCENT_EQUALS -> palette.onAccent
        GlassKeyStyle.OPERATOR -> palette.textPrimary
        GlassKeyStyle.FUNCTION -> palette.textPrimary
        GlassKeyStyle.DIGIT -> palette.textPrimary
    }

    val shadowColor = if (style == GlassKeyStyle.ACCENT_EQUALS) {
        palette.accentPrimary.copy(alpha = 0.48f)
    } else {
        palette.outerShadow
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (style == GlassKeyStyle.ACCENT_EQUALS) 12.dp else 6.dp,
                shape = CircleShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(CircleShape)
            .drawBehind {
                val radius = size.minDimension / 2f
                val focalX = size.width * (0.34f + 0.16f * sheenBoost)
                val focalY = size.height * (0.28f + 0.14f * sheenBoost)

                if (style == GlassKeyStyle.ACCENT_EQUALS) {
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.accentGlow,
                                palette.accentPrimary,
                                palette.accentSecondary
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )
                    // Luminous inner glass highlight
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f + 0.14f * sheenBoost),
                                Color.Transparent
                            ),
                            center = Offset(focalX, focalY),
                            radius = radius * 0.95f
                        )
                    )
                } else {
                    // Translucent radial glass base
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(centerColor, edgeColor),
                            center = Offset(focalX, focalY),
                            radius = radius * 1.35f
                        )
                    )
                    // Subtle liquid glass highlight pulse on touch
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f + 0.16f * sheenBoost),
                                Color.Transparent
                            ),
                            center = Offset(focalX, focalY),
                            radius = radius * 0.9f
                        )
                    )
                }

                // Soft upper-left inner specular arc reflection
                val arcInset = 2.dp.toPx()
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(
                                alpha = if (style == GlassKeyStyle.ACCENT_EQUALS) 0.45f
                                else if (palette.isDark) 0.22f else 0.7f
                            ),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.7f, size.height * 0.55f)
                    ),
                    startAngle = 200f,
                    sweepAngle = 115f,
                    useCenter = false,
                    topLeft = Offset(arcInset, arcInset),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - arcInset * 2,
                        size.height - arcInset * 2
                    ),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Thin directional outer glass rim border
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (style == GlassKeyStyle.ACCENT_EQUALS) Color.White.copy(alpha = 0.42f)
                            else palette.borderHighlight,
                            palette.borderSubtle,
                            if (palette.isDark) Color.White.copy(alpha = 0.08f)
                            else Color.Black.copy(alpha = 0.06f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    radius = radius - 0.6.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .semantics {
                role = Role.Button
                if (accessibilityLabel != null) {
                    contentDescription = accessibilityLabel
                }
                onClick(label = accessibilityLabel) {
                    currentOnClick()
                    true
                }
            }
            .pointerInput(onLongPressRepeat != null, onLongClickOnce != null) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    var longActionTriggered = false
                    val repeatJob = if (currentOnRepeat != null || currentOnLongOnce != null) {
                        scope.launch {
                            delay(420L)
                            longActionTriggered = true
                            val once = currentOnLongOnce
                            val rep = currentOnRepeat
                            if (once != null) {
                                once()
                            } else if (rep != null) {
                                while (true) {
                                    rep()
                                    delay(75L)
                                }
                            }
                        }
                    } else {
                        null
                    }

                    val up = waitForUpOrCancellation()
                    repeatJob?.cancel()
                    isPressed = false
                    if (up != null && !longActionTriggered) {
                        currentOnClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content(contentColor)
    }
}

/**
 * Pill-shaped Liquid Glass key for Scientific functions and compact tool chips.
 */
@Composable
fun LiquidGlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    accessibilityLabel: String? = null,
    content: @Composable BoxScope.(contentColor: Color) -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    var isPressed by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
        label = "pill_scale"
    )

    val bgTint by animateColorAsState(
        targetValue = if (isActive) palette.accentPrimary.copy(alpha = 0.28f)
        else palette.functionGlassCenter.copy(alpha = 0.72f),
        label = "pill_tint"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) palette.accentPrimary.copy(alpha = 0.75f)
        else palette.borderHighlight,
        label = "pill_border"
    )

    val contentColor = if (isActive) palette.accentGlow else palette.textPrimary

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgTint,
                        if (isActive) palette.accentSecondary.copy(alpha = 0.22f)
                        else palette.functionGlassEdge.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, palette.borderSubtle)
                ),
                shape = RoundedCornerShape(50)
            )
            .semantics {
                role = Role.Button
                if (accessibilityLabel != null) {
                    contentDescription = accessibilityLabel
                }
                onClick(label = accessibilityLabel) {
                    currentOnClick()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        currentOnClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content(contentColor)
    }
}

/**
 * Translucent Liquid Glass card container for History, Currency Converter, and Settings.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val baseColor = if (elevated) palette.cardSurfaceElevated else palette.cardSurface

    val baseModifier = modifier
        .shadow(
            elevation = if (elevated) 10.dp else 4.dp,
            shape = shape,
            ambientColor = palette.outerShadow,
            spotColor = palette.outerShadow
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor,
                    baseColor.copy(alpha = (baseColor.alpha * 0.92f).coerceIn(0f, 1f))
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    palette.borderHighlight,
                    palette.borderSubtle
                )
            ),
            shape = shape
        )

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = androidx.compose.material3.ripple(color = palette.accentGlow),
            role = Role.Button,
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(modifier = finalModifier, content = content)
}

@Composable
fun LiquidGlassSubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalLiquidGlassPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.functionGlassCenter.copy(alpha = 0.55f))
                    .border(1.dp, palette.borderHighlight, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back),
                    tint = palette.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = actions
        )
    }
}

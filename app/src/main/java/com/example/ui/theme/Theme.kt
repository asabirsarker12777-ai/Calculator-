package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AppThemeMode

data class LiquidGlassPalette(
    val isDark: Boolean,
    val background: Color,
    val cardSurface: Color,
    val cardSurfaceElevated: Color,
    val digitGlassCenter: Color,
    val digitGlassEdge: Color,
    val functionGlassCenter: Color,
    val functionGlassEdge: Color,
    val operatorGlassCenter: Color,
    val operatorGlassEdge: Color,
    val borderHighlight: Color,
    val borderSubtle: Color,
    val innerReflection: Color,
    val outerShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val onAccent: Color
)

val LocalLiquidGlassPalette = staticCompositionLocalOf {
    buildLiquidGlassPalette(isDark = true, accent = AccentColorOption.AMBER_ORANGE)
}

fun buildLiquidGlassPalette(
    isDark: Boolean,
    accent: AccentColorOption
): LiquidGlassPalette {
    val accentPrimary = Color(accent.primaryHex)
    val accentSecondary = Color(accent.secondaryHex)
    val accentGlow = Color(accent.glowHex)

    return if (isDark) {
        LiquidGlassPalette(
            isDark = true,
            background = ObsidianBackground,
            cardSurface = ObsidianSurface.copy(alpha = 0.86f),
            cardSurfaceElevated = ObsidianElevated.copy(alpha = 0.90f),
            digitGlassCenter = GlassDigitCenter.copy(alpha = 0.88f),
            digitGlassEdge = GlassDigitEdge.copy(alpha = 0.92f),
            functionGlassCenter = GlassFunctionCenter.copy(alpha = 0.90f),
            functionGlassEdge = GlassFunctionEdge.copy(alpha = 0.92f),
            operatorGlassCenter = GlassOperatorCenter.copy(alpha = 0.92f),
            operatorGlassEdge = GlassOperatorEdge.copy(alpha = 0.95f),
            borderHighlight = PureWhite.copy(alpha = 0.24f),
            borderSubtle = PureWhite.copy(alpha = 0.05f),
            innerReflection = PureWhite.copy(alpha = 0.14f),
            outerShadow = Color.Black.copy(alpha = 0.55f),
            textPrimary = PureWhite,
            textSecondary = TextSecondaryDark,
            textTertiary = TextTertiaryDark,
            accentPrimary = accentPrimary,
            accentSecondary = accentSecondary,
            accentGlow = accentGlow,
            onAccent = PureWhite
        )
    } else {
        LiquidGlassPalette(
            isDark = false,
            background = LightGlassBackground,
            cardSurface = LightGlassSurface.copy(alpha = 0.84f),
            cardSurfaceElevated = LightGlassSurface.copy(alpha = 0.96f),
            digitGlassCenter = LightGlassDigitCenter,
            digitGlassEdge = LightGlassDigitEdge,
            functionGlassCenter = LightGlassFunctionCenter,
            functionGlassEdge = LightGlassFunctionEdge,
            operatorGlassCenter = LightGlassOperatorCenter,
            operatorGlassEdge = LightGlassOperatorEdge,
            borderHighlight = PureWhite.copy(alpha = 0.95f),
            borderSubtle = Color.Black.copy(alpha = 0.08f),
            innerReflection = PureWhite.copy(alpha = 0.65f),
            outerShadow = Color.Black.copy(alpha = 0.12f),
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textTertiary = TextTertiaryLight,
            accentPrimary = accentPrimary,
            accentSecondary = accentSecondary,
            accentGlow = accentGlow,
            onAccent = PureWhite
        )
    }
}

private val AbirShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun AbirCalculatorTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentOption: AccentColorOption = AccentColorOption.AMBER_ORANGE,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }

    val glassPalette = buildLiquidGlassPalette(isDark = useDark, accent = accentOption)

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = glassPalette.accentPrimary,
            onPrimary = PureWhite,
            secondary = glassPalette.accentGlow,
            background = ObsidianBackground,
            onBackground = PureWhite,
            surface = ObsidianSurface,
            onSurface = PureWhite,
            surfaceVariant = ObsidianElevated,
            onSurfaceVariant = TextSecondaryDark,
            error = ErrorCoral
        )
    } else {
        lightColorScheme(
            primary = glassPalette.accentPrimary,
            onPrimary = PureWhite,
            secondary = glassPalette.accentGlow,
            background = LightGlassBackground,
            onBackground = TextPrimaryLight,
            surface = LightGlassSurface,
            onSurface = TextPrimaryLight,
            surfaceVariant = LightGlassDigitEdge,
            onSurfaceVariant = TextSecondaryLight,
            error = ErrorCoral
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalLiquidGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AbirTypography,
            shapes = AbirShapes,
            content = content
        )
    }
}

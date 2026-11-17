package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AngleMode
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.HapticStrength
import com.example.domain.model.NumberFormatStyle
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.LiquidGlassSubScreenTopBar
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.SuccessMint
import com.example.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current

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
                title = stringResource(R.string.title_settings),
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance Section
                SettingsSectionHeader(stringResource(R.string.section_appearance))
                LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppThemeMode.entries.forEach { mode ->
                                    val selected = settings.themeMode == mode
                                    LiquidGlassPillButton(
                                        onClick = { viewModel.setThemeMode(mode) },
                                        isActive = selected,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("theme_${mode.name}")
                                    ) { color ->
                                        Text(
                                            text = when (mode) {
                                                AppThemeMode.DARK -> "Dark"
                                                AppThemeMode.LIGHT -> "Light"
                                                AppThemeMode.SYSTEM -> "System"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Accent Color",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = settings.accentColor.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentGlow
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AccentColorOption.entries.forEach { option ->
                                    val isSelected = settings.accentColor == option
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(option.glowHex),
                                                        Color(option.secondaryHex)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else palette.borderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.setAccentColor(option) }
                                            .testTag("accent_${option.name}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = option.label,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Haptics & Sound Section
                SettingsSectionHeader(stringResource(R.string.section_haptics_audio))
                LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Haptic Feedback Strength",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Tap an option to preview native tactile response.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HapticStrength.entries.forEach { strength ->
                                    val selected = settings.hapticStrength == strength
                                    LiquidGlassPillButton(
                                        onClick = { viewModel.setHapticStrength(strength) },
                                        isActive = selected,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("haptic_${strength.name}")
                                    ) { color ->
                                        Text(
                                            text = strength.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Keypad Sound Effects",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Subtle native system click on key press",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                            Switch(
                                checked = settings.soundEffectsEnabled,
                                onCheckedChange = { viewModel.setSoundEffectsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.accentPrimary
                                ),
                                modifier = Modifier.testTag("settings_sound_switch")
                            )
                        }
                    }
                }

                // Calculation & Formatting Section
                SettingsSectionHeader(stringResource(R.string.section_calculation))
                LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Default Angle Unit",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AngleMode.entries.forEach { mode ->
                                    val selected = settings.angleMode == mode
                                    LiquidGlassPillButton(
                                        onClick = { viewModel.setAngleMode(mode) },
                                        isActive = selected,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) { color ->
                                        Text(
                                            text = if (mode == AngleMode.DEG) "Degrees (DEG)" else "Radians (RAD)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Decimal Precision",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "${settings.decimalPrecision} digits",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.accentGlow
                                )
                            }
                            Slider(
                                value = settings.decimalPrecision.toFloat(),
                                onValueChange = { viewModel.setDecimalPrecision(it.roundToInt()) },
                                valueRange = 2f..10f,
                                steps = 7,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.accentGlow,
                                    activeTrackColor = palette.accentPrimary
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Number Formatting",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NumberFormatStyle.entries.forEach { style ->
                                    val selected = settings.numberFormatStyle == style
                                    LiquidGlassPillButton(
                                        onClick = { viewModel.setNumberFormatStyle(style) },
                                        isActive = selected,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                    ) { color ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = style.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = color
                                            )
                                            Text(
                                                text = style.example,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) palette.accentGlow else palette.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Privacy & Offline Guarantee
                SettingsSectionHeader(stringResource(R.string.section_privacy))
                LiquidGlassCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SuccessMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = SuccessMint
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "100% Offline & Private",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Calculations, history, settings, and currency conversions stay on your device. No accounts, no analytics, no ads.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val palette = LocalLiquidGlassPalette.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = palette.accentGlow,
        modifier = Modifier.padding(start = 6.dp)
    )
}

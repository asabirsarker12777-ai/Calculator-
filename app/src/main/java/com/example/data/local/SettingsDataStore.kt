package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AngleMode
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.HapticStrength
import com.example.domain.model.NumberFormatStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "abir_calculator_settings"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val HAPTIC_STRENGTH = stringPreferencesKey("haptic_strength")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val NUMBER_FORMAT_STYLE = stringPreferencesKey("number_format_style")
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val ANGLE_MODE = stringPreferencesKey("angle_mode")
        val SCIENTIFIC_EXPANDED = booleanPreferencesKey("scientific_expanded")
        val LAST_FROM_CURRENCY = stringPreferencesKey("last_from_currency")
        val LAST_TO_CURRENCY = stringPreferencesKey("last_to_currency")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.DARK,
            accentColor = prefs[Keys.ACCENT_COLOR]?.let { runCatching { AccentColorOption.valueOf(it) }.getOrNull() }
                ?: AccentColorOption.AMBER_ORANGE,
            hapticStrength = prefs[Keys.HAPTIC_STRENGTH]?.let { runCatching { HapticStrength.valueOf(it) }.getOrNull() }
                ?: HapticStrength.MEDIUM,
            decimalPrecision = (prefs[Keys.DECIMAL_PRECISION] ?: 8).coerceIn(2, 10),
            numberFormatStyle = prefs[Keys.NUMBER_FORMAT_STYLE]?.let { runCatching { NumberFormatStyle.valueOf(it) }.getOrNull() }
                ?: NumberFormatStyle.STANDARD,
            soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS_ENABLED] ?: false,
            angleMode = prefs[Keys.ANGLE_MODE]?.let { runCatching { AngleMode.valueOf(it) }.getOrNull() }
                ?: AngleMode.DEG,
            isScientificExpanded = prefs[Keys.SCIENTIFIC_EXPANDED] ?: false
        )
    }

    val currencyPairFlow: Flow<Pair<String, String>> = context.settingsDataStore.data.map { prefs ->
        val from = prefs[Keys.LAST_FROM_CURRENCY] ?: "USD"
        val to = prefs[Keys.LAST_TO_CURRENCY] ?: "BDT"
        from to to
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(color: AccentColorOption) {
        context.settingsDataStore.edit { it[Keys.ACCENT_COLOR] = color.name }
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        context.settingsDataStore.edit { it[Keys.HAPTIC_STRENGTH] = strength.name }
    }

    suspend fun setDecimalPrecision(precision: Int) {
        context.settingsDataStore.edit { it[Keys.DECIMAL_PRECISION] = precision.coerceIn(2, 10) }
    }

    suspend fun setNumberFormatStyle(style: NumberFormatStyle) {
        context.settingsDataStore.edit { it[Keys.NUMBER_FORMAT_STYLE] = style.name }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SOUND_EFFECTS_ENABLED] = enabled }
    }

    suspend fun setAngleMode(mode: AngleMode) {
        context.settingsDataStore.edit { it[Keys.ANGLE_MODE] = mode.name }
    }

    suspend fun setScientificExpanded(expanded: Boolean) {
        context.settingsDataStore.edit { it[Keys.SCIENTIFIC_EXPANDED] = expanded }
    }

    suspend fun setCurrencyPair(fromCode: String, toCode: String) {
        context.settingsDataStore.edit {
            it[Keys.LAST_FROM_CURRENCY] = fromCode
            it[Keys.LAST_TO_CURRENCY] = toCode
        }
    }
}

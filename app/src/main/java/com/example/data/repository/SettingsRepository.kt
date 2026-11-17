package com.example.data.repository

import com.example.data.local.SettingsDataStore
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AngleMode
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.HapticStrength
import com.example.domain.model.NumberFormatStyle
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dataStore: SettingsDataStore) {
    val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow
    val currencyPairFlow: Flow<Pair<String, String>> = dataStore.currencyPairFlow

    suspend fun setThemeMode(mode: AppThemeMode) = dataStore.setThemeMode(mode)
    suspend fun setAccentColor(color: AccentColorOption) = dataStore.setAccentColor(color)
    suspend fun setHapticStrength(strength: HapticStrength) = dataStore.setHapticStrength(strength)
    suspend fun setDecimalPrecision(precision: Int) = dataStore.setDecimalPrecision(precision)
    suspend fun setNumberFormatStyle(style: NumberFormatStyle) = dataStore.setNumberFormatStyle(style)
    suspend fun setSoundEffectsEnabled(enabled: Boolean) = dataStore.setSoundEffectsEnabled(enabled)
    suspend fun setAngleMode(mode: AngleMode) = dataStore.setAngleMode(mode)
    suspend fun setScientificExpanded(expanded: Boolean) = dataStore.setScientificExpanded(expanded)
    suspend fun setCurrencyPair(fromCode: String, toCode: String) =
        dataStore.setCurrencyPair(fromCode, toCode)
}

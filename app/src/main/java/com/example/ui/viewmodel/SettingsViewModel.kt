package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AngleMode
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.HapticStrength
import com.example.domain.model.NumberFormatStyle
import com.example.ui.feedback.HapticFeedbackManager
import com.example.ui.feedback.TactileEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val hapticManager: HapticFeedbackManager
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: AppThemeMode) {
        previewHaptic(TactileEvent.MODE_CHANGE)
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAccentColor(accent: AccentColorOption) {
        previewHaptic(TactileEvent.MODE_CHANGE)
        viewModelScope.launch { settingsRepository.setAccentColor(accent) }
    }

    fun setHapticStrength(strength: HapticStrength) {
        val sound = settingsState.value.soundEffectsEnabled
        hapticManager.trigger(TactileEvent.OPERATOR, strength, sound)
        viewModelScope.launch { settingsRepository.setHapticStrength(strength) }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch { settingsRepository.setDecimalPrecision(precision) }
    }

    fun setNumberFormatStyle(style: NumberFormatStyle) {
        previewHaptic(TactileEvent.NUMBER)
        viewModelScope.launch { settingsRepository.setNumberFormatStyle(style) }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        val strength = settingsState.value.hapticStrength
        hapticManager.trigger(TactileEvent.NUMBER, strength, enabled)
        viewModelScope.launch { settingsRepository.setSoundEffectsEnabled(enabled) }
    }

    fun setAngleMode(mode: AngleMode) {
        previewHaptic(TactileEvent.MODE_CHANGE)
        viewModelScope.launch { settingsRepository.setAngleMode(mode) }
    }

    private fun previewHaptic(event: TactileEvent) {
        val s = settingsState.value
        hapticManager.trigger(event, s.hapticStrength, s.soundEffectsEnabled)
    }
}

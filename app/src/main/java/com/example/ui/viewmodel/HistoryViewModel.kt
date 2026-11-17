package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CalculatorRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.CalculationHistoryItem
import com.example.ui.feedback.HapticFeedbackManager
import com.example.ui.feedback.TactileEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val searchQuery: String = "",
    val items: List<CalculationHistoryItem> = emptyList(),
    val settings: AppSettings = AppSettings()
)

class HistoryViewModel(
    private val calculatorRepository: CalculatorRepository,
    private val settingsRepository: SettingsRepository,
    private val hapticManager: HapticFeedbackManager
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredHistoryFlow = searchQuery.flatMapLatest { q ->
        calculatorRepository.observeHistory(q)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        searchQuery,
        filteredHistoryFlow,
        settingsRepository.settingsFlow
    ) { q, items, settings ->
        HistoryUiState(
            searchQuery = q,
            items = items,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onDeleteItem(id: Long) {
        val s = uiState.value.settings
        hapticManager.trigger(TactileEvent.BACKSPACE, s.hapticStrength, s.soundEffectsEnabled)
        viewModelScope.launch {
            calculatorRepository.deleteHistoryItem(id)
        }
    }

    fun onClearAll() {
        val s = uiState.value.settings
        hapticManager.trigger(TactileEvent.CLEAR_ALL, s.hapticStrength, s.soundEffectsEnabled)
        viewModelScope.launch {
            calculatorRepository.clearHistory()
        }
    }
}

package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.engine.NumberFormatter
import com.example.domain.model.AppSettings
import com.example.domain.model.CurrencyRate
import com.example.domain.model.RecentConversion
import com.example.ui.feedback.HapticFeedbackManager
import com.example.ui.feedback.TactileEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurrencyUiState(
    val amountInput: String = "100",
    val formattedInput: String = "100",
    val fromCode: String = "USD",
    val toCode: String = "BDT",
    val convertedFormatted: String = "11,950.00",
    val convertedRaw: Double = 11950.0,
    val unitRateDisplay: String = "1 USD = 119.50 BDT",
    val rates: List<CurrencyRate> = emptyList(),
    val filteredRates: List<CurrencyRate> = emptyList(),
    val recentConversions: List<RecentConversion> = emptyList(),
    val searchQuery: String = "",
    val lastUpdatedMillis: Long = 0L,
    val isRefreshingOnline: Boolean = false,
    val statusMessage: String? = null,
    val settings: AppSettings = AppSettings()
)

private data class CurrencyLocalInput(
    val amountInput: String = "100",
    val searchQuery: String = "",
    val isRefreshingOnline: Boolean = false,
    val statusMessage: String? = null
)

class CurrencyViewModel(
    private val currencyRepository: CurrencyRepository,
    private val settingsRepository: SettingsRepository,
    private val hapticManager: HapticFeedbackManager
) : ViewModel() {

    private val localInput = MutableStateFlow(CurrencyLocalInput())

    val uiState: StateFlow<CurrencyUiState> = combine(
        localInput,
        currencyRepository.ratesFlow,
        currencyRepository.recentConversionsFlow,
        settingsRepository.currencyPairFlow,
        settingsRepository.settingsFlow
    ) { input, rates, recents, pair, settings ->
        val (fromCode, toCode) = pair
        val fromRate = rates.firstOrNull { it.code == fromCode } ?: rates.firstOrNull()
        val toRate = rates.firstOrNull { it.code == toCode } ?: rates.getOrNull(1) ?: fromRate

        val parsedAmount = input.amountInput.toDoubleOrNull() ?: 0.0
        val unitRate = if (fromRate != null && toRate != null && fromRate.ratePerUsd > 0.0) {
            toRate.ratePerUsd / fromRate.ratePerUsd
        } else {
            1.0
        }
        val converted = parsedAmount * unitRate

        val query = input.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            rates
        } else {
            rates.filter {
                it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
            }
        }

        val latestTimestamp = listOfNotNull(
            fromRate?.lastUpdatedMillis,
            toRate?.lastUpdatedMillis
        ).maxOrNull() ?: rates.maxOfOrNull { it.lastUpdatedMillis } ?: 0L

        val formattedUnitRate = NumberFormatter.formatCurrencyAmount(unitRate, settings.numberFormatStyle)
        val unitText = if (fromRate != null && toRate != null) {
            "1 ${fromRate.code} = $formattedUnitRate ${toRate.code}"
        } else {
            ""
        }

        CurrencyUiState(
            amountInput = input.amountInput,
            formattedInput = if (input.amountInput.isEmpty()) "0"
            else NumberFormatter.formatDisplayExpression(input.amountInput, settings.numberFormatStyle),
            fromCode = fromRate?.code ?: fromCode,
            toCode = toRate?.code ?: toCode,
            convertedFormatted = NumberFormatter.formatCurrencyAmount(converted, settings.numberFormatStyle),
            convertedRaw = converted,
            unitRateDisplay = unitText,
            rates = rates,
            filteredRates = filtered,
            recentConversions = recents,
            searchQuery = input.searchQuery,
            lastUpdatedMillis = latestTimestamp,
            isRefreshingOnline = input.isRefreshingOnline,
            statusMessage = input.statusMessage,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CurrencyUiState()
    )

    private fun triggerHaptic(event: TactileEvent) {
        val s = uiState.value.settings
        hapticManager.trigger(event, s.hapticStrength, s.soundEffectsEnabled)
    }

    fun onKeypadDigit(digit: String) {
        triggerHaptic(TactileEvent.NUMBER)
        localInput.update { state ->
            val current = state.amountInput
            val next = when {
                current == "0" && (digit == "0" || digit == "00") -> "0"
                current == "0" -> digit
                current.length >= 14 -> current
                current.isEmpty() && digit == "00" -> "0"
                else -> current + digit
            }
            state.copy(amountInput = next)
        }
    }

    fun onKeypadDecimal() {
        triggerHaptic(TactileEvent.NUMBER)
        localInput.update { state ->
            if (state.amountInput.contains('.')) return@update state
            val next = if (state.amountInput.isEmpty()) "0." else "${state.amountInput}."
            state.copy(amountInput = next)
        }
    }

    fun onKeypadBackspace() {
        triggerHaptic(TactileEvent.BACKSPACE)
        localInput.update { state ->
            val next = state.amountInput.dropLast(1)
            state.copy(amountInput = if (next.isEmpty()) "0" else next)
        }
    }

    fun onKeypadClear() {
        triggerHaptic(TactileEvent.CLEAR_ALL)
        localInput.update { it.copy(amountInput = "0") }
    }

    fun onAmountTextChanged(text: String) {
        val sanitized = buildString {
            var hasDot = false
            for (ch in text) {
                if (ch.isDigit()) append(ch)
                else if (ch == '.' && !hasDot) {
                    hasDot = true
                    append(ch)
                }
            }
        }
        localInput.update { it.copy(amountInput = sanitized.ifEmpty { "0" }) }
    }

    fun onSwapCurrencies() {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        val current = uiState.value
        viewModelScope.launch {
            settingsRepository.setCurrencyPair(current.toCode, current.fromCode)
        }
    }

    fun onSelectFromCurrency(code: String) {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        val current = uiState.value
        viewModelScope.launch {
            if (code == current.toCode) {
                settingsRepository.setCurrencyPair(code, current.fromCode)
            } else {
                settingsRepository.setCurrencyPair(code, current.toCode)
            }
        }
    }

    fun onSelectToCurrency(code: String) {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        val current = uiState.value
        viewModelScope.launch {
            if (code == current.fromCode) {
                settingsRepository.setCurrencyPair(current.toCode, code)
            } else {
                settingsRepository.setCurrencyPair(current.fromCode, code)
            }
        }
    }

    fun onToggleFavorite(code: String, isFavorite: Boolean) {
        triggerHaptic(TactileEvent.NUMBER)
        viewModelScope.launch {
            currencyRepository.toggleFavorite(code, isFavorite)
        }
    }

    fun onSearchQueryChanged(query: String) {
        localInput.update { it.copy(searchQuery = query) }
    }

    fun onSaveRecentConversion() {
        val s = uiState.value
        val amount = s.amountInput.toDoubleOrNull() ?: return
        if (amount <= 0.0) return
        triggerHaptic(TactileEvent.EQUALS)
        viewModelScope.launch {
            currencyRepository.saveRecentConversion(
                fromCode = s.fromCode,
                toCode = s.toCode,
                amount = amount,
                convertedAmount = s.convertedRaw
            )
            localInput.update { it.copy(statusMessage = "Saved to recent conversions") }
        }
    }

    fun onApplyRecentConversion(recent: RecentConversion) {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        val plainAmount = NumberFormatter.formatPlainDouble(recent.amount, 4)
        localInput.update { it.copy(amountInput = plainAmount) }
        viewModelScope.launch {
            settingsRepository.setCurrencyPair(recent.fromCode, recent.toCode)
        }
    }

    fun onUpdateManualRate(code: String, newRatePerUsd: Double) {
        triggerHaptic(TactileEvent.EQUALS)
        viewModelScope.launch {
            currencyRepository.updateManualRate(code, newRatePerUsd)
            localInput.update { it.copy(statusMessage = "Updated offline reference rate for $code") }
        }
    }

    fun onResetRateToDefault(code: String) {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        viewModelScope.launch {
            currencyRepository.resetCurrencyToDefault(code)
            localInput.update { it.copy(statusMessage = "Restored default rate for $code") }
        }
    }

    fun onRefreshOnlineRates() {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        if (localInput.value.isRefreshingOnline) return
        localInput.update { it.copy(isRefreshingOnline = true, statusMessage = null) }
        viewModelScope.launch {
            val msg = currencyRepository.refreshRatesOnline(overwriteManualEdits = false)
            localInput.update { it.copy(isRefreshingOnline = false, statusMessage = msg) }
        }
    }

    fun dismissStatusMessage() {
        localInput.update { it.copy(statusMessage = null) }
    }
}

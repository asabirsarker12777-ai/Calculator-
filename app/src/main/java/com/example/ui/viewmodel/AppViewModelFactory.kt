package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.AppContainer

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CalculatorViewModel::class.java) -> {
                CalculatorViewModel(
                    engine = container.mathEngine,
                    calculatorRepository = container.calculatorRepository,
                    settingsRepository = container.settingsRepository,
                    hapticManager = container.hapticManager
                ) as T
            }
            modelClass.isAssignableFrom(CurrencyViewModel::class.java) -> {
                CurrencyViewModel(
                    currencyRepository = container.currencyRepository,
                    settingsRepository = container.settingsRepository,
                    hapticManager = container.hapticManager
                ) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(
                    calculatorRepository = container.calculatorRepository,
                    settingsRepository = container.settingsRepository,
                    hapticManager = container.hapticManager
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    hapticManager = container.hapticManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

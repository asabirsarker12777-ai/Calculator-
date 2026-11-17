package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.OnlineRateFetcher
import com.example.data.repository.CalculatorRepository
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.engine.MathExpressionEngine
import com.example.ui.feedback.HapticFeedbackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    private val database: AppDatabase by lazy { AppDatabase.getInstance(application) }
    private val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(application) }
    private val onlineRateFetcher: OnlineRateFetcher by lazy { OnlineRateFetcher(application) }

    val mathEngine: MathExpressionEngine by lazy { MathExpressionEngine() }
    val hapticManager: HapticFeedbackManager by lazy { HapticFeedbackManager(application) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(settingsDataStore) }
    val calculatorRepository: CalculatorRepository by lazy {
        CalculatorRepository(database.historyDao())
    }
    val currencyRepository: CurrencyRepository by lazy {
        CurrencyRepository(database.currencyDao(), onlineRateFetcher)
    }
}

class AbirCalculatorApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.currencyRepository.ensureSeeded()
        }
    }
}

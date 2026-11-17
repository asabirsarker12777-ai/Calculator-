package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CurrencyConverterScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AbirCalculatorTheme
import com.example.ui.viewmodel.AppViewModelFactory
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.CurrencyViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.SettingsViewModel

object NavRoutes {
    const val CALCULATOR = "calculator"
    const val CURRENCY = "currency"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AbirCalculatorApp
        val factory = AppViewModelFactory(app.container)

        setContent {
            AbirCalculatorRoot(factory = factory)
        }
    }
}

@Composable
fun AbirCalculatorRoot(factory: AppViewModelFactory) {
    val navController = rememberNavController()
    val calcViewModel: CalculatorViewModel = viewModel(factory = factory)
    val calcState by calcViewModel.uiState.collectAsStateWithLifecycle()

    AbirCalculatorTheme(
        themeMode = calcState.settings.themeMode,
        accentOption = calcState.settings.accentColor
    ) {
        NavHost(
            navController = navController,
            startDestination = NavRoutes.CALCULATOR,
            enterTransition = {
                slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220))
            },
            exitTransition = {
                fadeOut(tween(160))
            },
            popEnterTransition = {
                fadeIn(tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(tween(220)) { it / 5 } + fadeOut(tween(180))
            }
        ) {
            composable(NavRoutes.CALCULATOR) {
                CalculatorScreen(
                    uiState = calcState,
                    viewModel = calcViewModel,
                    onNavigateToCurrency = {
                        navController.navigate(NavRoutes.CURRENCY) { launchSingleTop = true }
                    },
                    onNavigateToHistory = {
                        navController.navigate(NavRoutes.HISTORY) { launchSingleTop = true }
                    },
                    onNavigateToSettings = {
                        navController.navigate(NavRoutes.SETTINGS) { launchSingleTop = true }
                    }
                )
            }

            composable(NavRoutes.CURRENCY) {
                val currencyViewModel: CurrencyViewModel = viewModel(factory = factory)
                val currencyState by currencyViewModel.uiState.collectAsStateWithLifecycle()
                CurrencyConverterScreen(
                    uiState = currencyState,
                    viewModel = currencyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.HISTORY) {
                val historyViewModel: HistoryViewModel = viewModel(factory = factory)
                val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()
                HistoryScreen(
                    uiState = historyState,
                    viewModel = historyViewModel,
                    onReuseItem = { item ->
                        calcViewModel.onReuseHistoryItem(item)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings = settingsState,
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

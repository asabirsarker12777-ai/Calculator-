package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CalculatorRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.engine.EvaluationResult
import com.example.domain.engine.MathExpressionEngine
import com.example.domain.engine.NumberFormatter
import com.example.domain.model.AngleMode
import com.example.domain.model.AppSettings
import com.example.domain.model.CalculationHistoryItem
import com.example.ui.feedback.HapticFeedbackManager
import com.example.ui.feedback.TactileEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val rawExpression: String = "",
    val displayExpression: String = "0",
    val previousExpression: String = "",
    val previewOrResult: String = "",
    val lastValidPlainResult: String = "0",
    val errorMessage: String? = null,
    val justEvaluated: Boolean = false,
    val isDisplayExpanded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val recentHistory: List<CalculationHistoryItem> = emptyList()
)

private data class InternalCalcState(
    val rawExpression: String = "",
    val previousExpression: String = "",
    val evaluatedDisplay: String = "",
    val lastValidPlainResult: String = "0",
    val errorMessage: String? = null,
    val justEvaluated: Boolean = false,
    val isDisplayExpanded: Boolean = false,
    val scientificOverride: Boolean? = null
)

class CalculatorViewModel(
    private val engine: MathExpressionEngine,
    private val calculatorRepository: CalculatorRepository,
    private val settingsRepository: SettingsRepository,
    private val hapticManager: HapticFeedbackManager
) : ViewModel() {

    private val internalState = MutableStateFlow(InternalCalcState())

    val uiState: StateFlow<CalculatorUiState> = combine(
        internalState,
        settingsRepository.settingsFlow,
        calculatorRepository.observeHistory()
    ) { internal, settings, history ->
        val effectiveSettings = internal.scientificOverride?.let {
            settings.copy(isScientificExpanded = it)
        } ?: settings

        val formattedExpr = if (internal.rawExpression.isEmpty()) {
            "0"
        } else {
            NumberFormatter.formatDisplayExpression(
                internal.rawExpression,
                effectiveSettings.numberFormatStyle
            )
        }

        val preview = if (internal.justEvaluated) {
            internal.evaluatedDisplay
        } else if (internal.rawExpression.isEmpty()) {
            ""
        } else {
            when (
                val eval = engine.evaluatePreview(
                    expression = internal.rawExpression,
                    angleMode = effectiveSettings.angleMode,
                    precision = effectiveSettings.decimalPrecision,
                    numberFormatStyle = effectiveSettings.numberFormatStyle
                )
            ) {
                is EvaluationResult.Success -> eval.formatted
                is EvaluationResult.Error -> eval.message
                EvaluationResult.Empty -> ""
            }
        }

        CalculatorUiState(
            rawExpression = internal.rawExpression,
            displayExpression = formattedExpr,
            previousExpression = internal.previousExpression,
            previewOrResult = preview,
            lastValidPlainResult = internal.lastValidPlainResult,
            errorMessage = internal.errorMessage,
            justEvaluated = internal.justEvaluated,
            isDisplayExpanded = internal.isDisplayExpanded,
            settings = effectiveSettings,
            recentHistory = history.take(4)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculatorUiState()
    )

    private fun triggerHaptic(event: TactileEvent) {
        val currentSettings = uiState.value.settings
        hapticManager.trigger(
            event = event,
            strength = currentSettings.hapticStrength,
            soundEnabled = currentSettings.soundEffectsEnabled
        )
    }

    fun onDigit(digit: String) {
        triggerHaptic(TactileEvent.NUMBER)
        internalState.update { state ->
            val base = if (state.justEvaluated) "" else state.rawExpression
            val currentToken = extractTrailingNumberToken(base)

            val nextExpr = when {
                base.isEmpty() && (digit == "0" || digit == "00") -> "0"
                currentToken == "0" && (digit == "0" || digit == "00") -> base
                currentToken == "0" -> base.dropLast(1) + digit
                base.endsWith(")") || base.endsWith("π") || base.endsWith("e") ||
                    base.endsWith("²") || base.endsWith("³") || base.endsWith("!") ||
                    base.endsWith("%") -> base + "×" + (if (digit == "00") "0" else digit)
                else -> base + digit
            }
            state.copy(
                rawExpression = nextExpr,
                justEvaluated = false,
                errorMessage = null,
                evaluatedDisplay = ""
            )
        }
    }

    fun onDecimal() {
        triggerHaptic(TactileEvent.NUMBER)
        internalState.update { state ->
            val base = if (state.justEvaluated) "" else state.rawExpression
            val trailingNum = extractTrailingNumberToken(base)
            if (trailingNum.contains('.')) {
                return@update state
            }
            val nextExpr = when {
                base.isEmpty() -> "0."
                base.last() in listOf('+', '−', '-', '×', '÷', '^', '(') -> "${base}0."
                base.last() in listOf(')', 'π', 'e', '²', '³', '!', '%') -> "${base}×0."
                else -> "$base."
            }
            state.copy(
                rawExpression = nextExpr,
                justEvaluated = false,
                errorMessage = null,
                evaluatedDisplay = ""
            )
        }
    }

    fun onOperator(op: String) {
        triggerHaptic(TactileEvent.OPERATOR)
        internalState.update { state ->
            val base = if (state.justEvaluated) {
                if (state.errorMessage == null && state.lastValidPlainResult.isNotEmpty()) {
                    state.lastValidPlainResult
                } else {
                    ""
                }
            } else {
                state.rawExpression
            }

            if (base.isEmpty()) {
                return@update if (op == "−") {
                    state.copy(
                        rawExpression = "-",
                        justEvaluated = false,
                        errorMessage = null,
                        evaluatedDisplay = ""
                    )
                } else {
                    state
                }
            }

            // Handle negative exponent like 2^-3
            if (op == "−" && base.endsWith("^")) {
                return@update state.copy(
                    rawExpression = "$base-",
                    justEvaluated = false,
                    errorMessage = null,
                    evaluatedDisplay = ""
                )
            }

            if (op == "−" && base.endsWith("(")) {
                return@update state.copy(
                    rawExpression = "$base-",
                    justEvaluated = false,
                    errorMessage = null,
                    evaluatedDisplay = ""
                )
            }

            val binaryOps = listOf('+', '−', '-', '×', '÷', '^')
            val nextExpr = when {
                base == "-" && op != "−" -> ""
                base.endsWith("(") -> base
                base.last() in binaryOps -> {
                    // If ends with "^-" and user presses another op, drop both
                    if (base.length >= 2 && base[base.length - 2] == '^' && base.last() == '-') {
                        base.dropLast(2) + op
                    } else {
                        base.dropLast(1) + op
                    }
                }
                else -> base + op
            }

            state.copy(
                rawExpression = nextExpr,
                justEvaluated = false,
                errorMessage = null,
                evaluatedDisplay = ""
            )
        }
    }

    fun onPercent() {
        triggerHaptic(TactileEvent.OPERATOR)
        internalState.update { state ->
            val base = if (state.justEvaluated) state.lastValidPlainResult else state.rawExpression
            if (base.isEmpty()) return@update state
            val last = base.last()
            if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '²' || last == '³' || last == '!') {
                state.copy(
                    rawExpression = "$base%",
                    justEvaluated = false,
                    errorMessage = null,
                    evaluatedDisplay = ""
                )
            } else {
                state
            }
        }
    }

    fun onBackspace() {
        triggerHaptic(TactileEvent.BACKSPACE)
        internalState.update { state ->
            if (state.justEvaluated) {
                return@update state.copy(
                    rawExpression = "",
                    evaluatedDisplay = "",
                    errorMessage = null,
                    justEvaluated = false
                )
            }
            val expr = state.rawExpression
            if (expr.isEmpty()) return@update state

            val functionTokens = listOf(
                "sin⁻¹(", "cos⁻¹(", "tan⁻¹(",
                "asin(", "acos(", "atan(",
                "sqrt(", "sin(", "cos(", "tan(",
                "log(", "abs(", "ln(", "√("
            )
            val matchedFn = functionTokens.firstOrNull { expr.endsWith(it) }
            val nextExpr = if (matchedFn != null) {
                expr.dropLast(matchedFn.length)
            } else {
                expr.dropLast(1)
            }
            state.copy(
                rawExpression = nextExpr,
                errorMessage = null
            )
        }
    }

    fun onClearAll() {
        triggerHaptic(TactileEvent.CLEAR_ALL)
        internalState.update {
            it.copy(
                rawExpression = "",
                previousExpression = "",
                evaluatedDisplay = "",
                lastValidPlainResult = "0",
                errorMessage = null,
                justEvaluated = false
            )
        }
    }

    fun onEquals() {
        val snapshot = uiState.value
        val expr = snapshot.rawExpression.trim()
        if (expr.isEmpty() || snapshot.justEvaluated) {
            triggerHaptic(TactileEvent.EQUALS)
            return
        }

        triggerHaptic(TactileEvent.EQUALS)
        val settings = snapshot.settings
        when (
            val result = engine.evaluate(
                expression = expr,
                angleMode = settings.angleMode,
                precision = settings.decimalPrecision,
                numberFormatStyle = settings.numberFormatStyle
            )
        ) {
            is EvaluationResult.Success -> {
                val formattedExpr = NumberFormatter.formatDisplayExpression(
                    expr,
                    settings.numberFormatStyle
                )
                internalState.update {
                    it.copy(
                        rawExpression = result.rawPlain,
                        previousExpression = "$formattedExpr =",
                        evaluatedDisplay = result.formatted,
                        lastValidPlainResult = result.rawPlain,
                        errorMessage = null,
                        justEvaluated = true
                    )
                }

                // Persist meaningful expressions to Room history
                if (hasOperationOrFunction(expr)) {
                    viewModelScope.launch {
                        calculatorRepository.saveCalculation(
                            expression = formattedExpr,
                            formattedResult = result.formatted,
                            rawResult = result.rawPlain,
                            isScientific = isScientificExpression(expr)
                        )
                    }
                }
            }
            is EvaluationResult.Error -> {
                internalState.update {
                    it.copy(
                        evaluatedDisplay = result.message,
                        errorMessage = result.message,
                        justEvaluated = true
                    )
                }
            }
            EvaluationResult.Empty -> Unit
        }
    }

    fun onScientificInput(action: String) {
        triggerHaptic(TactileEvent.OPERATOR)
        internalState.update { state ->
            val base = if (state.justEvaluated) {
                if (state.errorMessage == null) state.lastValidPlainResult else ""
            } else {
                state.rawExpression
            }

            val nextExpr = when (action) {
                "sin", "cos", "tan", "sin⁻¹", "cos⁻¹", "tan⁻¹", "log", "ln", "√", "abs" -> {
                    val token = "$action("
                    if (state.justEvaluated && base.isNotEmpty()) {
                        "$token$base)"
                    } else if (base.isEmpty() || base.last() in listOf('+', '−', '-', '×', '÷', '^', '(')) {
                        "$base$token"
                    } else {
                        "${base}×$token"
                    }
                }
                "x²" -> {
                    if (base.isNotEmpty() && canAcceptPostfix(base)) "${base}²" else base
                }
                "x³" -> {
                    if (base.isNotEmpty() && canAcceptPostfix(base)) "${base}³" else base
                }
                "xʸ" -> {
                    if (base.isNotEmpty() && canAcceptPostfix(base)) "${base}^" else base
                }
                "x!" -> {
                    if (base.isNotEmpty() && canAcceptPostfix(base)) "${base}!" else base
                }
                "1/x" -> {
                    if (base.isEmpty()) {
                        "1÷("
                    } else if (state.justEvaluated) {
                        "1÷($base)"
                    } else if (base.last() in listOf('+', '−', '-', '×', '÷', '^', '(')) {
                        "${base}1÷("
                    } else {
                        "${base}^(-1)"
                    }
                }
                "π", "e" -> {
                    if (state.justEvaluated || base.isEmpty()) {
                        action
                    } else if (base.last() in listOf('+', '−', '-', '×', '÷', '^', '(')) {
                        "$base$action"
                    } else {
                        "${base}×$action"
                    }
                }
                "(" -> {
                    if (state.justEvaluated || base.isEmpty()) {
                        "("
                    } else if (base.last() in listOf('+', '−', '-', '×', '÷', '^', '(')) {
                        "$base("
                    } else {
                        "${base}×("
                    }
                }
                ")" -> {
                    val openCount = base.count { it == '(' }
                    val closeCount = base.count { it == ')' }
                    if (openCount > closeCount && base.isNotEmpty() &&
                        base.last() !in listOf('+', '−', '-', '×', '÷', '^', '(')
                    ) {
                        "$base)"
                    } else {
                        base
                    }
                }
                else -> base
            }

            state.copy(
                rawExpression = nextExpr,
                justEvaluated = false,
                errorMessage = null,
                evaluatedDisplay = ""
            )
        }
    }

    fun onToggleScientificMode() {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        val target = !uiState.value.settings.isScientificExpanded
        internalState.update { it.copy(scientificOverride = target) }
        viewModelScope.launch {
            settingsRepository.setScientificExpanded(target)
        }
    }

    fun onToggleDisplayExpanded() {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        internalState.update { it.copy(isDisplayExpanded = !it.isDisplayExpanded) }
    }

    fun onToggleAngleMode() {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        val next = if (uiState.value.settings.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        viewModelScope.launch {
            settingsRepository.setAngleMode(next)
        }
    }

    fun onPasteText(clipboardText: String): Boolean {
        val cleaned = clipboardText
            .trim()
            .replace(",", "")
            .replace(" ", "")
            .replace("−", "-")
        val parsed = cleaned.toDoubleOrNull() ?: return false
        val plain = NumberFormatter.formatPlainDouble(parsed, uiState.value.settings.decimalPrecision)

        triggerHaptic(TactileEvent.NUMBER)
        internalState.update { state ->
            val base = if (state.justEvaluated) "" else state.rawExpression
            val next = if (base.isEmpty()) {
                plain
            } else if (base.last() in listOf('+', '−', '-', '×', '÷', '^', '(')) {
                "$base$plain"
            } else {
                "${base}×$plain"
            }
            state.copy(
                rawExpression = next,
                justEvaluated = false,
                errorMessage = null,
                evaluatedDisplay = ""
            )
        }
        return true
    }

    fun onReuseHistoryItem(item: CalculationHistoryItem) {
        triggerHaptic(TactileEvent.MODE_CHANGE)
        internalState.update {
            it.copy(
                rawExpression = item.rawResult,
                previousExpression = "${item.expression} =",
                evaluatedDisplay = item.result,
                lastValidPlainResult = item.rawResult,
                errorMessage = null,
                justEvaluated = true
            )
        }
    }

    fun onModeSwitchHaptic() {
        triggerHaptic(TactileEvent.MODE_CHANGE)
    }

    fun getCopyableResult(): String {
        val state = uiState.value
        return when {
            state.errorMessage != null -> ""
            state.justEvaluated && state.previewOrResult.isNotEmpty() -> state.previewOrResult
            state.previewOrResult.isNotEmpty() && state.previewOrResult != "Cannot divide by 0" -> state.previewOrResult
            state.rawExpression.isNotEmpty() -> state.displayExpression
            else -> "0"
        }
    }

    private fun extractTrailingNumberToken(expr: String): String {
        if (expr.isEmpty()) return ""
        var idx = expr.length - 1
        while (idx >= 0 && (expr[idx].isDigit() || expr[idx] == '.')) {
            idx--
        }
        return expr.substring(idx + 1)
    }

    private fun canAcceptPostfix(expr: String): Boolean {
        val last = expr.last()
        return last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '²' || last == '³' || last == '!'
    }

    private fun hasOperationOrFunction(expr: String): Boolean {
        return expr.any { it in listOf('+', '−', '-', '×', '÷', '^', '%', '!', '²', '³', '√', 'π', 'e') } ||
            expr.contains("sin") || expr.contains("cos") || expr.contains("tan") ||
            expr.contains("log") || expr.contains("ln") || expr.contains("abs")
    }

    private fun isScientificExpression(expr: String): Boolean {
        return expr.any { it in listOf('^', '!', '²', '³', '√', 'π', 'e', '(') } ||
            expr.contains("sin") || expr.contains("cos") || expr.contains("tan") ||
            expr.contains("log") || expr.contains("ln") || expr.contains("abs")
    }
}

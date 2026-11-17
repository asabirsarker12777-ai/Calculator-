package com.example.domain.engine

import com.example.domain.model.NumberFormatStyle
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object NumberFormatter {

    fun formatBigDecimal(
        value: BigDecimal,
        precision: Int,
        style: NumberFormatStyle
    ): String {
        val clampedPrecision = precision.coerceIn(2, 10)
        val rounded = value.setScale(clampedPrecision, RoundingMode.HALF_UP).stripTrailingZeros()
        val absVal = rounded.abs()

        if (absVal > BigDecimal.ZERO &&
            (absVal >= BigDecimal("1E12") || absVal < BigDecimal("1E-7"))
        ) {
            return formatScientific(rounded.toDouble(), clampedPrecision, style)
        }

        val plain = if (rounded.compareTo(BigDecimal.ZERO) == 0) "0" else rounded.toPlainString()
        return applyNumberStyle(plain, style)
    }

    fun formatDouble(
        value: Double,
        precision: Int,
        style: NumberFormatStyle
    ): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val clampedPrecision = precision.coerceIn(2, 10)
        val cleanValue = if (abs(value) < 1e-13) 0.0 else value
        if (cleanValue == 0.0) return "0"

        val absVal = abs(cleanValue)
        if (absVal >= 1e12 || (absVal > 0.0 && absVal < 1e-7)) {
            return formatScientific(cleanValue, clampedPrecision, style)
        }

        val bd = BigDecimal.valueOf(cleanValue)
            .setScale(clampedPrecision, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val plain = if (bd.compareTo(BigDecimal.ZERO) == 0) "0" else bd.toPlainString()
        return applyNumberStyle(plain, style)
    }

    fun formatPlainDouble(value: Double, precision: Int): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val clean = if (abs(value) < 1e-13) 0.0 else value
        if (clean == 0.0) return "0"
        val absVal = abs(clean)
        if (absVal >= 1e12 || absVal < 1e-7) {
            return formatScientific(clean, precision.coerceIn(2, 10), NumberFormatStyle.PLAIN)
        }
        val bd = BigDecimal.valueOf(clean)
            .setScale(precision.coerceIn(2, 10), RoundingMode.HALF_UP)
            .stripTrailingZeros()
        return if (bd.compareTo(BigDecimal.ZERO) == 0) "0" else bd.toPlainString()
    }

    fun formatCurrencyAmount(value: Double, style: NumberFormatStyle): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val absVal = abs(value)
        val decimals = when {
            absVal == 0.0 -> 2
            absVal < 0.01 -> 6
            absVal < 1.0 -> 4
            else -> 2
        }
        val bd = BigDecimal.valueOf(value)
            .setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val plain = if (bd.compareTo(BigDecimal.ZERO) == 0) "0.00" else bd.toPlainString()
        return applyNumberStyle(plain, style)
    }

    fun formatDisplayExpression(expression: String, style: NumberFormatStyle): String {
        if (expression.isEmpty() || style == NumberFormatStyle.PLAIN) return expression
        val groupChar = if (style == NumberFormatStyle.EUROPEAN) '.' else ','
        val decChar = if (style == NumberFormatStyle.EUROPEAN) ',' else '.'

        val result = StringBuilder()
        var i = 0
        while (i < expression.length) {
            val ch = expression[i]
            if (ch.isDigit() || ch == '.') {
                val numStart = i
                var hasDot = false
                while (i < expression.length && (expression[i].isDigit() || (expression[i] == '.' && !hasDot))) {
                    if (expression[i] == '.') hasDot = true
                    i++
                }
                val token = expression.substring(numStart, i)
                result.append(formatNumberToken(token, groupChar, decChar))
            } else {
                result.append(ch)
                i++
            }
        }
        return result.toString()
    }

    private fun formatNumberToken(token: String, groupChar: Char, decChar: Char): String {
        val parts = token.split('.', limit = 2)
        val intPart = parts[0]
        val groupedInt = if (intPart.length > 3) {
            val sb = StringBuilder()
            var count = 0
            for (idx in intPart.length - 1 downTo 0) {
                sb.append(intPart[idx])
                count++
                if (count % 3 == 0 && idx > 0) {
                    sb.append(groupChar)
                }
            }
            sb.reverse().toString()
        } else {
            intPart
        }
        return if (parts.size == 2) "$groupedInt$decChar${parts[1]}" else groupedInt
    }

    private fun applyNumberStyle(plain: String, style: NumberFormatStyle): String {
        if (style == NumberFormatStyle.PLAIN) return plain
        val isNeg = plain.startsWith("-")
        val body = if (isNeg) plain.substring(1) else plain
        val groupChar = if (style == NumberFormatStyle.EUROPEAN) '.' else ','
        val decChar = if (style == NumberFormatStyle.EUROPEAN) ',' else '.'
        val formatted = formatNumberToken(body, groupChar, decChar)
        return if (isNeg) "-$formatted" else formatted
    }

    private fun formatScientific(
        value: Double,
        precision: Int,
        style: NumberFormatStyle
    ): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            decimalSeparator = if (style == NumberFormatStyle.EUROPEAN) ',' else '.'
        }
        val hashes = "#".repeat((precision - 1).coerceIn(1, 8))
        val df = DecimalFormat("0.${hashes}E0", symbols)
        return df.format(value).replace("E", "×10^")
    }
}

package com.example.domain.engine

import com.example.domain.model.AngleMode
import com.example.domain.model.NumberFormatStyle
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

sealed class EvaluationResult {
    data class Success(
        val formatted: String,
        val rawPlain: String,
        val numericValue: Double
    ) : EvaluationResult()

    data class Error(val message: String) : EvaluationResult()
    data object Empty : EvaluationResult()
}

class MathExpressionEngine {

    private sealed interface Token {
        data class Number(val text: String) : Token
        data class Constant(val symbol: String) : Token
        data class Function(val name: String) : Token
        data class Operator(val symbol: Char) : Token
        data object LeftParen : Token
        data object RightParen : Token
        data object Factorial : Token
        data object Percent : Token
        data object Square : Token
        data object Cube : Token
    }

    private sealed interface ExprNode {
        data class Literal(val raw: String, val value: Double) : ExprNode
        data class UnaryNeg(val inner: ExprNode) : ExprNode
        data class UnaryPlus(val inner: ExprNode) : ExprNode
        data class Binary(val left: ExprNode, val op: Char, val right: ExprNode) : ExprNode
        data class Percent(val inner: ExprNode) : ExprNode
        data class Factorial(val inner: ExprNode) : ExprNode
        data class Power2(val inner: ExprNode) : ExprNode
        data class Power3(val inner: ExprNode) : ExprNode
        data class FuncCall(val name: String, val arg: ExprNode) : ExprNode
    }

    fun evaluate(
        expression: String,
        angleMode: AngleMode = AngleMode.DEG,
        precision: Int = 8,
        numberFormatStyle: NumberFormatStyle = NumberFormatStyle.STANDARD
    ): EvaluationResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return EvaluationResult.Empty

        return try {
            val tokens = tokenize(trimmed)
            if (tokens.isEmpty()) return EvaluationResult.Empty

            val normalized = autoCloseParentheses(insertImplicitMultiplication(tokens))
            val parser = Parser(normalized)
            val ast = parser.parse()

            if (!isPureBigDecimal(ast)) {
                val doubleResult = evalDouble(ast, angleMode)
                val snapped = snapTrigNoise(doubleResult)
                if (snapped.isNaN() || snapped.isInfinite()) {
                    return EvaluationResult.Error("Math error")
                }
                val formatted = NumberFormatter.formatDouble(snapped, precision, numberFormatStyle)
                val rawPlain = NumberFormatter.formatPlainDouble(snapped, precision)
                EvaluationResult.Success(formatted, rawPlain, snapped)
            } else {
                val bdResult = evalBigDecimal(ast)
                val formatted = NumberFormatter.formatBigDecimal(bdResult, precision, numberFormatStyle)
                val rawPlain = NumberFormatter.formatBigDecimal(bdResult, precision, NumberFormatStyle.PLAIN)
                EvaluationResult.Success(formatted, rawPlain, bdResult.toDouble())
            }
        } catch (e: DivisionByZeroException) {
            EvaluationResult.Error("Cannot divide by 0")
        } catch (e: MathDomainException) {
            EvaluationResult.Error(e.message ?: "Domain error")
        } catch (e: InvalidSyntaxException) {
            EvaluationResult.Error("Invalid expression")
        } catch (e: Exception) {
            EvaluationResult.Error("Invalid expression")
        }
    }

    fun evaluatePreview(
        expression: String,
        angleMode: AngleMode = AngleMode.DEG,
        precision: Int = 8,
        numberFormatStyle: NumberFormatStyle = NumberFormatStyle.STANDARD
    ): EvaluationResult {
        val cleaned = trimTrailingIncompleteOperators(expression.trim())
        if (cleaned.isEmpty()) return EvaluationResult.Empty
        // Avoid showing a redundant preview if the input is only a single literal number
        val tokens = try {
            tokenize(cleaned)
        } catch (_: Exception) {
            return EvaluationResult.Empty
        }
        if (tokens.size <= 1 && tokens.firstOrNull() is Token.Number) {
            return EvaluationResult.Empty
        }
        if (tokens.size == 2 && tokens[0] == Token.Operator('-') && tokens[1] is Token.Number) {
            return EvaluationResult.Empty
        }
        return when (val res = evaluate(cleaned, angleMode, precision, numberFormatStyle)) {
            is EvaluationResult.Success -> res
            is EvaluationResult.Error -> {
                if (res.message == "Cannot divide by 0") res else EvaluationResult.Empty
            }
            EvaluationResult.Empty -> EvaluationResult.Empty
        }
    }

    private fun trimTrailingIncompleteOperators(expr: String): String {
        var s = expr
        while (s.isNotEmpty()) {
            val last = s.last()
            if (last in listOf('+', '-', '−', '×', '*', '÷', '/', '^', '(')) {
                s = s.dropLast(1)
            } else {
                break
            }
        }
        // Also strip trailing incomplete function name like "sin"
        val funcs = listOf("asin", "acos", "atan", "sin⁻¹", "cos⁻¹", "tan⁻¹", "sin", "cos", "tan", "log", "ln", "abs", "sqrt", "√")
        for (fn in funcs) {
            if (s.endsWith(fn)) {
                s = s.dropLast(fn.length)
                return trimTrailingIncompleteOperators(s)
            }
        }
        return s
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = input.length

        while (i < len) {
            val ch = input[i]
            when {
                ch.isWhitespace() || ch == ',' -> i++

                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var hasDot = false
                    while (i < len && (input[i].isDigit() || (input[i] == '.' && !hasDot))) {
                        if (input[i] == '.') hasDot = true
                        i++
                    }
                    // Scientific E-notation support when pasted e.g. 1.2e-5
                    if (i < len && (input[i] == 'E' || input[i] == 'e') &&
                        i + 1 < len && (input[i + 1].isDigit() || input[i + 1] == '+' || input[i + 1] == '-')
                    ) {
                        val expStart = i
                        i++
                        if (i < len && (input[i] == '+' || input[i] == '-')) i++
                        val digitStart = i
                        while (i < len && input[i].isDigit()) i++
                        if (i == digitStart) {
                            i = expStart
                        }
                    }
                    var numText = input.substring(start, i)
                    if (numText == ".") numText = "0."
                    if (numText.startsWith(".")) numText = "0$numText"
                    tokens.add(Token.Number(numText))
                }

                ch == 'π' -> {
                    tokens.add(Token.Constant("π"))
                    i++
                }

                ch == 'e' -> {
                    tokens.add(Token.Constant("e"))
                    i++
                }

                ch == '√' -> {
                    tokens.add(Token.Function("sqrt"))
                    i++
                }

                ch == '²' -> {
                    tokens.add(Token.Square)
                    i++
                }

                ch == '³' -> {
                    tokens.add(Token.Cube)
                    i++
                }

                ch == '!' -> {
                    tokens.add(Token.Factorial)
                    i++
                }

                ch == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }

                ch == '(' -> {
                    tokens.add(Token.LeftParen)
                    i++
                }

                ch == ')' -> {
                    tokens.add(Token.RightParen)
                    i++
                }

                ch == '+' -> {
                    tokens.add(Token.Operator('+'))
                    i++
                }

                ch == '-' || ch == '−' -> {
                    tokens.add(Token.Operator('-'))
                    i++
                }

                ch == '×' || ch == '*' -> {
                    tokens.add(Token.Operator('*'))
                    i++
                }

                ch == '÷' || ch == '/' -> {
                    tokens.add(Token.Operator('/'))
                    i++
                }

                ch == '^' -> {
                    tokens.add(Token.Operator('^'))
                    i++
                }

                ch.isLetter() -> {
                    val matched = matchFunctionName(input, i)
                    if (matched != null) {
                        tokens.add(Token.Function(matched.first))
                        i += matched.second
                    } else {
                        throw InvalidSyntaxException("Unknown token at $i")
                    }
                }

                else -> throw InvalidSyntaxException("Unexpected char $ch")
            }
        }
        return tokens
    }

    private fun matchFunctionName(input: String, index: Int): Pair<String, Int>? {
        val candidates = listOf(
            "sin⁻¹" to "asin",
            "cos⁻¹" to "acos",
            "tan⁻¹" to "atan",
            "asin" to "asin",
            "acos" to "acos",
            "atan" to "atan",
            "sqrt" to "sqrt",
            "sin" to "sin",
            "cos" to "cos",
            "tan" to "tan",
            "log" to "log",
            "abs" to "abs",
            "ln" to "ln"
        )
        for ((prefix, normalized) in candidates) {
            if (input.startsWith(prefix, startIndex = index)) {
                return normalized to prefix.length
            }
        }
        return null
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val out = ArrayList<Token>(tokens.size + 4)
        for (i in tokens.indices) {
            val curr = tokens[i]
            if (i > 0) {
                val prev = tokens[i - 1]
                val prevEndsValue = prev is Token.Number ||
                    prev is Token.Constant ||
                    prev is Token.RightParen ||
                    prev is Token.Factorial ||
                    prev is Token.Percent ||
                    prev is Token.Square ||
                    prev is Token.Cube
                val currStartsValue = curr is Token.Number ||
                    curr is Token.Constant ||
                    curr is Token.Function ||
                    curr is Token.LeftParen

                if (prevEndsValue && currStartsValue) {
                    out.add(Token.Operator('*'))
                }
            }
            out.add(curr)
        }
        return out
    }

    private fun autoCloseParentheses(tokens: List<Token>): List<Token> {
        var balance = 0
        for (t in tokens) {
            if (t is Token.LeftParen) balance++
            else if (t is Token.RightParen) balance--
        }
        if (balance <= 0) return tokens
        val out = tokens.toMutableList()
        repeat(balance) { out.add(Token.RightParen) }
        return out
    }

    private inner class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun parse(): ExprNode {
            val node = parseAddSub()
            if (pos < tokens.size) {
                throw InvalidSyntaxException("Unexpected trailing tokens")
            }
            return node
        }

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        private fun parseAddSub(): ExprNode {
            var left = parseMulDiv()
            while (true) {
                val op = peek()
                if (op is Token.Operator && (op.symbol == '+' || op.symbol == '-')) {
                    consume()
                    val right = parseMulDiv()
                    left = ExprNode.Binary(left, op.symbol, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseMulDiv(): ExprNode {
            var left = parseUnary()
            while (true) {
                val op = peek()
                if (op is Token.Operator && (op.symbol == '*' || op.symbol == '/')) {
                    consume()
                    val right = parseUnary()
                    left = ExprNode.Binary(left, op.symbol, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): ExprNode {
            val tok = peek()
            if (tok is Token.Operator && tok.symbol == '-') {
                consume()
                return ExprNode.UnaryNeg(parseUnary())
            }
            if (tok is Token.Operator && tok.symbol == '+') {
                consume()
                return ExprNode.UnaryPlus(parseUnary())
            }
            return parsePower()
        }

        private fun parsePower(): ExprNode {
            val base = parsePostfix()
            val tok = peek()
            if (tok is Token.Operator && tok.symbol == '^') {
                consume()
                // Right-associative power, allowing unary minus in exponent (e.g., 2^-3)
                val exponent = parseUnary()
                return ExprNode.Binary(base, '^', exponent)
            }
            return base
        }

        private fun parsePostfix(): ExprNode {
            var node = parsePrimary()
            while (true) {
                when (peek()) {
                    is Token.Percent -> {
                        consume()
                        node = ExprNode.Percent(node)
                    }
                    is Token.Factorial -> {
                        consume()
                        node = ExprNode.Factorial(node)
                    }
                    is Token.Square -> {
                        consume()
                        node = ExprNode.Power2(node)
                    }
                    is Token.Cube -> {
                        consume()
                        node = ExprNode.Power3(node)
                    }
                    else -> break
                }
            }
            return node
        }

        private fun parsePrimary(): ExprNode {
            return when (val tok = peek()) {
                is Token.Number -> {
                    consume()
                    val d = tok.text.toDoubleOrNull()
                        ?: throw InvalidSyntaxException("Bad number ${tok.text}")
                    ExprNode.Literal(tok.text, d)
                }
                is Token.Constant -> {
                    consume()
                    when (tok.symbol) {
                        "π" -> ExprNode.Literal("π", Math.PI)
                        "e" -> ExprNode.Literal("e", Math.E)
                        else -> throw InvalidSyntaxException("Unknown constant")
                    }
                }
                is Token.Function -> {
                    consume()
                    val arg = if (peek() is Token.LeftParen) {
                        consume()
                        val inner = parseAddSub()
                        if (peek() !is Token.RightParen) {
                            throw InvalidSyntaxException("Missing )")
                        }
                        consume()
                        inner
                    } else {
                        parseUnary()
                    }
                    ExprNode.FuncCall(tok.name, arg)
                }
                is Token.LeftParen -> {
                    consume()
                    val inner = parseAddSub()
                    if (peek() !is Token.RightParen) {
                        throw InvalidSyntaxException("Missing )")
                    }
                    consume()
                    inner
                }
                else -> throw InvalidSyntaxException("Unexpected token $tok")
            }
        }
    }

    private fun isPureBigDecimal(node: ExprNode): Boolean {
        return when (node) {
            is ExprNode.Literal -> node.raw != "π" && node.raw != "e"
            is ExprNode.UnaryNeg -> isPureBigDecimal(node.inner)
            is ExprNode.UnaryPlus -> isPureBigDecimal(node.inner)
            is ExprNode.Percent -> isPureBigDecimal(node.inner)
            is ExprNode.Power2 -> isPureBigDecimal(node.inner)
            is ExprNode.Power3 -> isPureBigDecimal(node.inner)
            is ExprNode.Factorial -> false
            is ExprNode.FuncCall -> node.name == "abs" && isPureBigDecimal(node.arg)
            is ExprNode.Binary -> {
                if (node.op == '^') {
                    isPureBigDecimal(node.left) &&
                        node.right is ExprNode.Literal &&
                        node.right.raw.toIntOrNull()?.let { it in 0..20 } == true
                } else {
                    isPureBigDecimal(node.left) && isPureBigDecimal(node.right)
                }
            }
        }
    }

    private fun evalBigDecimal(node: ExprNode): BigDecimal {
        val mc = MathContext.DECIMAL128
        return when (node) {
            is ExprNode.Literal -> BigDecimal(node.raw, mc)
            is ExprNode.UnaryNeg -> evalBigDecimal(node.inner).negate(mc)
            is ExprNode.UnaryPlus -> evalBigDecimal(node.inner)
            is ExprNode.Percent -> evalBigDecimal(node.inner).divide(BigDecimal(100), mc)
            is ExprNode.Power2 -> {
                val v = evalBigDecimal(node.inner)
                v.multiply(v, mc)
            }
            is ExprNode.Power3 -> {
                val v = evalBigDecimal(node.inner)
                v.multiply(v, mc).multiply(v, mc)
            }
            is ExprNode.FuncCall -> {
                if (node.name == "abs") evalBigDecimal(node.arg).abs()
                else throw MathDomainException("Unsupported")
            }
            is ExprNode.Binary -> {
                val l = evalBigDecimal(node.left)
                if ((node.op == '+' || node.op == '-') && node.right is ExprNode.Percent) {
                    val pctVal = evalBigDecimal(node.right.inner)
                    val delta = l.multiply(pctVal, mc).divide(BigDecimal(100), mc)
                    return if (node.op == '+') l.add(delta, mc) else l.subtract(delta, mc)
                }
                val r = evalBigDecimal(node.right)
                when (node.op) {
                    '+' -> l.add(r, mc)
                    '-' -> l.subtract(r, mc)
                    '*' -> l.multiply(r, mc)
                    '/' -> {
                        if (r.compareTo(BigDecimal.ZERO) == 0) {
                            throw DivisionByZeroException()
                        }
                        l.divide(r, mc)
                    }
                    '^' -> {
                        val exp = r.toInt()
                        l.pow(exp, mc)
                    }
                    else -> throw InvalidSyntaxException("Unknown op")
                }
            }
            is ExprNode.Factorial -> throw InvalidSyntaxException("Use double")
        }
    }

    private fun evalDouble(node: ExprNode, angleMode: AngleMode): Double {
        return when (node) {
            is ExprNode.Literal -> node.value
            is ExprNode.UnaryNeg -> -evalDouble(node.inner, angleMode)
            is ExprNode.UnaryPlus -> evalDouble(node.inner, angleMode)
            is ExprNode.Percent -> evalDouble(node.inner, angleMode) / 100.0
            is ExprNode.Power2 -> {
                val v = evalDouble(node.inner, angleMode)
                v * v
            }
            is ExprNode.Power3 -> {
                val v = evalDouble(node.inner, angleMode)
                v * v * v
            }
            is ExprNode.Factorial -> {
                val v = evalDouble(node.inner, angleMode)
                if (v < 0 || v != kotlin.math.floor(v) || v > 170.0) {
                    throw MathDomainException("Factorial requires 0 ≤ n ≤ 170")
                }
                var acc = 1.0
                val n = v.toInt()
                for (k in 2..n) {
                    acc *= k
                }
                acc
            }
            is ExprNode.Binary -> {
                val l = evalDouble(node.left, angleMode)
                if ((node.op == '+' || node.op == '-') && node.right is ExprNode.Percent) {
                    val pct = evalDouble(node.right.inner, angleMode)
                    val delta = l * (pct / 100.0)
                    return if (node.op == '+') l + delta else l - delta
                }
                val r = evalDouble(node.right, angleMode)
                when (node.op) {
                    '+' -> l + r
                    '-' -> l - r
                    '*' -> l * r
                    '/' -> {
                        if (abs(r) == 0.0) throw DivisionByZeroException()
                        l / r
                    }
                    '^' -> {
                        val res = l.pow(r)
                        if (res.isNaN()) throw MathDomainException("Domain error")
                        res
                    }
                    else -> throw InvalidSyntaxException("Unknown op")
                }
            }
            is ExprNode.FuncCall -> {
                val x = evalDouble(node.arg, angleMode)
                when (node.name) {
                    "sin" -> sin(toRadians(x, angleMode))
                    "cos" -> cos(toRadians(x, angleMode))
                    "tan" -> {
                        val rad = toRadians(x, angleMode)
                        val c = cos(rad)
                        if (abs(c) < 1e-12) throw MathDomainException("Undefined (tan)")
                        tan(rad)
                    }
                    "asin" -> {
                        if (x < -1.0 || x > 1.0) throw MathDomainException("Domain: -1 ≤ x ≤ 1")
                        fromRadians(asin(x), angleMode)
                    }
                    "acos" -> {
                        if (x < -1.0 || x > 1.0) throw MathDomainException("Domain: -1 ≤ x ≤ 1")
                        fromRadians(acos(x), angleMode)
                    }
                    "atan" -> fromRadians(atan(x), angleMode)
                    "log" -> {
                        if (x <= 0.0) throw MathDomainException("Domain: x > 0")
                        log10(x)
                    }
                    "ln" -> {
                        if (x <= 0.0) throw MathDomainException("Domain: x > 0")
                        ln(x)
                    }
                    "sqrt" -> {
                        if (x < 0.0) throw MathDomainException("Domain: x ≥ 0")
                        sqrt(x)
                    }
                    "abs" -> abs(x)
                    else -> throw InvalidSyntaxException("Unknown function ${node.name}")
                }
            }
        }
    }

    private fun toRadians(value: Double, mode: AngleMode): Double {
        return if (mode == AngleMode.DEG) Math.toRadians(value) else value
    }

    private fun fromRadians(rad: Double, mode: AngleMode): Double {
        return if (mode == AngleMode.DEG) Math.toDegrees(rad) else rad
    }

    private fun snapTrigNoise(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return value
        if (abs(value) < 1e-12) return 0.0
        val nearestInt = kotlin.math.round(value)
        if (abs(value - nearestInt) < 1e-11) return nearestInt
        return value
    }

    private class DivisionByZeroException : RuntimeException()
    private class MathDomainException(msg: String) : RuntimeException(msg)
    private class InvalidSyntaxException(msg: String) : RuntimeException(msg)
}

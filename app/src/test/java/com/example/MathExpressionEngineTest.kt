package com.example

import com.example.domain.engine.EvaluationResult
import com.example.domain.engine.MathExpressionEngine
import com.example.domain.model.AngleMode
import com.example.domain.model.NumberFormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExpressionEngineTest {

    private val engine = MathExpressionEngine()

    @Test
    fun `basic arithmetic and floating point precision`() {
        val add = engine.evaluate("0.1+0.2", numberFormatStyle = NumberFormatStyle.PLAIN)
        assertTrue(add is EvaluationResult.Success)
        assertEquals("0.3", (add as EvaluationResult.Success).formatted)

        val precedence = engine.evaluate("2+3×4−6÷2")
        assertTrue(precedence is EvaluationResult.Success)
        assertEquals("11", (precedence as EvaluationResult.Success).formatted)
    }

    @Test
    fun `smart percentage calculations`() {
        val addPct = engine.evaluate("100+10%")
        assertEquals("110", (addPct as EvaluationResult.Success).formatted)

        val subPct = engine.evaluate("200−25%")
        assertEquals("150", (subPct as EvaluationResult.Success).formatted)

        val mulPct = engine.evaluate("500×10%")
        assertEquals("50", (mulPct as EvaluationResult.Success).formatted)

        val plainPct = engine.evaluate("50%")
        assertEquals("0.5", (plainPct as EvaluationResult.Success).formatted)
    }

    @Test
    fun `scientific functions in DEG and RAD modes`() {
        val sin30 = engine.evaluate("sin(30)", AngleMode.DEG)
        assertEquals("0.5", (sin30 as EvaluationResult.Success).formatted)

        val cos90 = engine.evaluate("cos(90)", AngleMode.DEG)
        assertEquals("0", (cos90 as EvaluationResult.Success).formatted)

        val sinPiHalf = engine.evaluate("sin(π÷2)", AngleMode.RAD)
        assertEquals("1", (sinPiHalf as EvaluationResult.Success).formatted)

        val invSin = engine.evaluate("sin⁻¹(0.5)", AngleMode.DEG)
        assertEquals("30", (invSin as EvaluationResult.Success).formatted)

        val fact = engine.evaluate("5!")
        assertEquals("120", (fact as EvaluationResult.Success).formatted)

        val logVal = engine.evaluate("log(1000)+ln(e)")
        assertEquals("4", (logVal as EvaluationResult.Success).formatted)

        val powAndSqrt = engine.evaluate("√(16)+2^3+3²")
        assertEquals("21", (powAndSqrt as EvaluationResult.Success).formatted)

        val absVal = engine.evaluate("abs(-42)")
        assertEquals("42", (absVal as EvaluationResult.Success).formatted)
    }

    @Test
    fun `division by zero and domain errors handled safely`() {
        val divZero = engine.evaluate("10÷0")
        assertTrue(divZero is EvaluationResult.Error)
        assertEquals("Cannot divide by 0", (divZero as EvaluationResult.Error).message)

        val negSqrt = engine.evaluate("√(-4)")
        assertTrue(negSqrt is EvaluationResult.Error)

        val badAsin = engine.evaluate("sin⁻¹(2)")
        assertTrue(badAsin is EvaluationResult.Error)
    }
}

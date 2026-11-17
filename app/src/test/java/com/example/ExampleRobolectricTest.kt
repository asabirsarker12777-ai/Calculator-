package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreloadedCurrencyData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name and preloaded offline currencies are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Abir Calculator", appName)

        val rates = PreloadedCurrencyData.getDefaultRates()
        val codes = rates.map { it.code }.toSet()
        val required = listOf(
            "USD", "BDT", "EUR", "GBP", "INR", "AED", "SAR",
            "CAD", "AUD", "JPY", "CNY", "SGD", "MYR", "PKR"
        )
        assertTrue(codes.containsAll(required))
    }

    @Test
    fun `calculator 4x5 keypad performs calculation end to end`() {
        composeTestRule.waitForIdle()

        // Verify exact 4x5 keypad buttons exist and compute 12 × 5 = 60
        composeTestRule.onNodeWithTag("btn_ac").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_percent").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_backspace").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_divide").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_00").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_1").performClick()
        composeTestRule.onNodeWithTag("btn_2").performClick()
        composeTestRule.onNodeWithTag("btn_multiply").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        composeTestRule.onNodeWithTag("btn_equals").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calc_display_main", useUnmergedTree = true)
            .assertTextContains("60")
    }

    @Test
    fun `scientific mode toggle computes sin(30) in DEG`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_scientific").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sci_sin").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sci_sin").performClick()
        composeTestRule.onNodeWithTag("btn_3").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        composeTestRule.onNodeWithTag("btn_equals").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calc_display_main", useUnmergedTree = true)
            .assertTextContains("0.5")
    }
}

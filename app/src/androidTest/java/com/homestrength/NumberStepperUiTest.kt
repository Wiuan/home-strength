package com.homestrength

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.homestrength.ui.components.NumberStepper
import com.homestrength.ui.theme.HomeStrengthTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NumberStepperUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun plusButtonIncrementsDisplayedValue() {
        var value: Int? by mutableStateOf(5)
        composeRule.setContent {
            HomeStrengthTheme {
                NumberStepper(
                    value = value,
                    onValueChange = { value = it }
                )
            }
        }
        composeRule.onNodeWithText("5").assertTextEquals("5")
        composeRule.onNodeWithText("+").performClick()
        composeRule.onNodeWithText("6").assertExists()
    }
}

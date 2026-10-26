package io.github.mangi.eta.agent.overlay

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.mangi.eta.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AgentOverlayCapsuleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun coldStartIslandRequestOpensInputWithoutTogglingCollapsed() {
        var toggleCount = 0
        val inputModes = mutableListOf<Boolean>()
        setCapsule(
            expanded = true,
            onToggleExpanded = { toggleCount++ },
            onSupplementModeChange = inputModes::add,
        )

        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText(context.getString(R.string.overlay_supplement_hint)).assertIsDisplayed()
        assertEquals(0, toggleCount)
        assertEquals(listOf(true), inputModes)
    }

    @Test
    fun cancelIslandInputFinishesExternalSession() {
        var finishCount = 0
        setCapsule(expanded = true, onExternalSupplementFinished = { finishCount++ })

        composeRule.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        composeRule.mainClock.advanceTimeBy(100)

        composeRule.waitUntil(timeoutMillis = 2_000) { finishCount == 1 }
    }

    @Test
    fun sendIslandInputDeliversTextBeforeFinishing() {
        val callbacks = mutableListOf<String>()
        setCapsule(
            expanded = true,
            onSupplement = { callbacks += "send:$it" },
            onExternalSupplementFinished = { callbacks += "finish" },
        )

        composeRule.onNode(hasSetTextAction()).performTextInput("continue")
        composeRule.onNodeWithText(context.getString(R.string.overlay_send)).performClick()
        composeRule.mainClock.advanceTimeBy(100)

        composeRule.waitUntil(timeoutMillis = 2_000) { "finish" in callbacks }
        assertEquals(listOf("send:continue", "finish"), callbacks)
        assertTrue(callbacks.indexOf("send:continue") < callbacks.indexOf("finish"))
    }

    private fun setCapsule(
        expanded: Boolean,
        onToggleExpanded: () -> Unit = {},
        onSupplementModeChange: (Boolean) -> Unit = {},
        onSupplement: (String) -> Unit = {},
        onExternalSupplementFinished: () -> Unit = {},
    ) {
        val expandedState = mutableStateOf(expanded)
        composeRule.setContent {
            MiuixTheme(colors = lightColorScheme()) {
                CompositionLocalProvider(LocalSquircleEnabled provides false) {
                    AgentOverlayCapsule(
                        state = AgentOverlayState.Initial,
                        expanded = expandedState.value,
                        supplementRequestCount = 1,
                        yielded = false,
                        onToggleExpanded = {
                            expandedState.value = !expandedState.value
                            onToggleExpanded()
                        },
                        onCollapsedSettled = {},
                        onPause = {},
                        onResume = {},
                        onStop = {},
                        onSupplementModeChange = onSupplementModeChange,
                        onSupplement = onSupplement,
                        onExternalSupplementFinished = onExternalSupplementFinished,
                    )
                }
            }
        }
    }
}

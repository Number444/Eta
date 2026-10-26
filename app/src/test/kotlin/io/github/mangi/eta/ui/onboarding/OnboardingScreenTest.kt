package io.github.mangi.eta.ui.onboarding

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import io.github.mangi.eta.R
import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.db.EtaDatabase
import io.github.mangi.eta.data.model.Model
import io.github.mangi.eta.data.model.ModelSource
import io.github.mangi.eta.data.model.OpenAiCompatibleProviderSetting
import io.github.mangi.eta.data.model.withModels
import io.github.mangi.eta.data.provider.BuiltinProviders
import io.github.mangi.eta.data.provider.OfficialModelCatalog
import io.github.mangi.eta.data.repository.ProviderRepository
import io.github.mangi.eta.data.repository.RemoteModelCatalog
import io.github.mangi.eta.ui.app.AgentAppTheme
import io.github.mangi.eta.data.model.AppearanceSettings
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "zh-rCN-w420dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSystemService(NotificationManager::class.java).cancelAll()
        EtaDatabase.closeForTests()
        context.deleteDatabase("eta.db")
        SettingsDataStore.init(context)
        ProviderRepository.init(context)
        runBlocking {
            SettingsDataStore.setSelection(null, null)
            SettingsDataStore.setOnboardingCompleted(false)
            val originalModel = Model(
                id = "original-model",
                modelId = "original-model",
                displayName = "Original model",
                source = ModelSource.MANUAL,
            )
            val added = ProviderRepository.addProvider(
                requireNotNull(BuiltinProviders.providerById(BuiltinProviders.KIMI_ID))
                    .withModels(listOf(originalModel)),
            )
            io.github.mangi.eta.data.repository.RuntimeConfigRepository
                .setSelectedModelId(added.models.single().id)
        }
    }

    @Test
    fun testModeWalksAllStepsWithoutChangingConfigurationOrLaunchingIntents() {
        val provider = runBlocking { ProviderRepository.allProviders().single() }
        val beforeProviders = listOf(provider)
        val beforeSettings = runBlocking { SettingsDataStore.settings() }
        val events = mutableListOf<Int>()
        var finished = false
        composeRule.setContent {
            AgentAppTheme(appearance = AppearanceSettings(), applyInterfaceScale = false) {
                OnboardingScreen(
                    testMode = true,
                    onFinished = { finished = true },
                    onTestEvent = events::add,
                )
            }
        }
        waitForText(provider.name)
        screenshot("provider")
        composeRule.onNodeWithText(provider.name).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_next)).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_step_connection)).assertIsDisplayed()
        screenshot("connection")
        composeRule.onNodeWithText(text(R.string.onboarding_next)).performClick()

        val sampleModel = OfficialModelCatalog.modelsForProvider(provider).first()
        waitForText(sampleModel.displayName)
        screenshot("model")
        composeRule.onNodeWithText(sampleModel.displayName).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_step_permissions)).assertIsDisplayed()
        screenshot("permissions")
        composeRule.onNodeWithText(text(R.string.capability_notifications_title)).performClick()
        composeRule.onNodeWithText(text(R.string.ui_floating_window_permissions_076b77)).performClick()
        composeRule.onNodeWithText(text(R.string.ui_accessibility_enhancement_tools_8fd257)).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_finish)).performClick()

        assertTrue(finished)
        assertEquals(
            listOf(
                R.string.onboarding_test_event_provider,
                R.string.onboarding_test_event_connection,
                R.string.onboarding_test_event_model,
                R.string.onboarding_test_event_permission,
                R.string.onboarding_test_event_permission,
                R.string.onboarding_test_event_permission,
                R.string.onboarding_test_event_finish,
            ),
            events,
        )
        assertEquals(beforeProviders, runBlocking { ProviderRepository.allProviders() })
        assertEquals(beforeSettings, runBlocking { SettingsDataStore.settings() })
        assertFalse(runBlocking { SettingsDataStore.onboardingCompleted() })
        assertNull(Shadows.shadowOf(context as android.app.Application).nextStartedActivity)
    }

    @Test
    fun realModelSaveUpdatesProviderCatalogAndSelection() = runBlocking {
        val original = ProviderRepository.allProviders().single() as OpenAiCompatibleProviderSetting
        val connection = original.copy(apiKey = "updated-key")
        val remote = Model(
            id = "remote-uuid",
            modelId = "remote-model",
            displayName = "Remote model",
            source = ModelSource.REMOTE,
        )
        saveOnboardingModel(
            provider = connection,
            isNew = false,
            catalog = RemoteModelCatalog(listOf(remote), connection, null),
            model = remote,
        )

        val savedProvider = requireNotNull(ProviderRepository.providerById(connection.id))
        val savedModel = savedProvider.models.single { it.modelId == remote.modelId }
        assertEquals("updated-key", savedProvider.apiKey)
        assertEquals(connection.id, SettingsDataStore.settings().selectedProviderId)
        assertEquals(savedModel.id, SettingsDataStore.settings().selectedModelId)
    }

    @Test
    fun testModePostsSystemNotificationWithoutSecrets() {
        val provider = runBlocking { ProviderRepository.allProviders().single() }
        composeRule.setContent {
            AgentAppTheme(appearance = AppearanceSettings(), applyInterfaceScale = false) {
                OnboardingScreen(testMode = true, onFinished = {})
            }
        }
        waitForText(provider.name)
        composeRule.onNodeWithText(provider.name).performClick()

        val manager = context.getSystemService(NotificationManager::class.java)
        composeRule.waitUntil(5_000) { Shadows.shadowOf(manager).allNotifications.isNotEmpty() }
        val posted = Shadows.shadowOf(manager).allNotifications.single()
        assertEquals(
            text(R.string.onboarding_test_notification_title),
            posted.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
        assertEquals(
            context.getString(
                R.string.onboarding_test_notification_text,
                text(R.string.onboarding_test_event_provider),
            ),
            posted.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
    }

    @Test
    fun realModeSkipMarksSetupCompleted() {
        var finished = false
        composeRule.setContent {
            AgentAppTheme(appearance = AppearanceSettings(), applyInterfaceScale = false) {
                key("real-skip") {
                    OnboardingScreen(testMode = false, onFinished = { finished = true })
                }
            }
        }
        composeRule.onNodeWithText(text(R.string.onboarding_skip)).performClick()
        composeRule.waitUntil(5_000) { finished }
        assertTrue(runBlocking { SettingsDataStore.onboardingCompleted() })
    }

    private fun waitForText(value: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun text(resource: Int): String = context.getString(resource)

    private fun screenshot(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val output = File("build/outputs/onboarding/$name.png")
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(output.length() > 0)
    }
}

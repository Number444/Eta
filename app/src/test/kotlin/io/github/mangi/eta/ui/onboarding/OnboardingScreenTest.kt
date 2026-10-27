package io.github.mangi.eta.ui.onboarding

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.sun.net.httpserver.HttpServer
import io.github.mangi.eta.R
import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.db.EtaDatabase
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.data.model.Model
import io.github.mangi.eta.data.model.ModelSource
import io.github.mangi.eta.data.model.OpenAiCompatibleProviderSetting
import io.github.mangi.eta.data.model.withModels
import io.github.mangi.eta.data.provider.BuiltinProviders
import io.github.mangi.eta.data.repository.ProviderRepository
import io.github.mangi.eta.data.repository.RemoteModelCatalog
import io.github.mangi.eta.data.repository.RuntimeConfigRepository
import io.github.mangi.eta.ui.app.AgentAppTheme
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    private lateinit var server: HttpServer

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        EtaDatabase.closeForTests()
        context.deleteDatabase("eta.db")
        SettingsDataStore.init(context)
        ProviderRepository.init(context)
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/v1/models") { exchange ->
                val body = """{"data":[
                  {"id":"original-model","display_name":"Original model"},
                  {"id":"second-model","display_name":"Second model"}
                ]}""".trimIndent().toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        runBlocking {
            SettingsDataStore.setOnboardingCompleted(false)
            val originalModel = Model(
                id = "original-local-id",
                modelId = "original-model",
                displayName = "Original model",
                source = ModelSource.MANUAL,
            )
            val provider = requireNotNull(BuiltinProviders.providerById(BuiltinProviders.KIMI_ID))
                    as OpenAiCompatibleProviderSetting
            val added = ProviderRepository.addProvider(
                provider.copy(
                    baseUrl = "http://127.0.0.1:${server.address.port}/v1",
                    apiKey = "test-key",
                ).withModels(listOf(originalModel)),
            )
            SettingsDataStore.setSelection(added.id, added.models.single().id)
            RuntimeConfigRepository.setSelectedModelId(added.models.single().id)
        }
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun providerPageSupportsGestureScrollingWithFullCatalog() {
        runBlocking {
            BuiltinProviders.PROVIDERS.forEach { ProviderRepository.addProvider(it) }
        }
        showOnboarding()
        repeat(6) {
            composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText(text(R.string.ui_new_anthropic_db6098)).assertIsDisplayed()
    }

    @Test
    fun realFlowPrefillsAndSavesThroughLocalCatalog() {
        val provider = runBlocking { ProviderRepository.allProviders().single() }
        var finished = false
        showOnboarding { finished = true }
        waitForText(provider.name)
        composeRule.onNodeWithText(provider.name).performClick()
        screenshot("provider-selected")
        next()
        composeRule.onNodeWithText(provider.baseUrl).assertIsDisplayed()
        screenshot("connection")
        next()

        waitForText("Original model")
        screenshot("model")
        next()
        waitForText(text(R.string.onboarding_step_permissions))
        screenshot("permissions")
        composeRule.onNodeWithText(text(R.string.ui_floating_window_permissions_076b77)).performClick()
        assertNotNull(Shadows.shadowOf(context as android.app.Application).nextStartedActivity)
        composeRule.onNodeWithText(text(R.string.onboarding_finish)).performClick()

        composeRule.waitUntil(5_000) { finished }
        assertTrue(runBlocking { SettingsDataStore.onboardingCompleted() })
        val savedProvider = runBlocking { ProviderRepository.providerById(provider.id) }
        assertNotNull(savedProvider)
        assertEquals(provider.apiKey, savedProvider!!.apiKey)
        assertEquals(provider.baseUrl, savedProvider.baseUrl)
        assertEquals("original-model", savedProvider.models.single().modelId)
        assertEquals(savedProvider.models.single().id, runBlocking { SettingsDataStore.settings().selectedModelId })
    }

    @Test
    fun selectingAnotherModelPersistsRemoteSelection() {
        showOnboarding()
        navigateToModel()
        composeRule.onNodeWithText("Second model").performClick()
        waitForText(text(R.string.onboarding_step_permissions))

        val provider = requireNotNull(runBlocking { ProviderRepository.providerById(BuiltinProviders.KIMI_ID) })
        val selected = provider.models.single { it.modelId == "second-model" }
        assertEquals(selected.id, runBlocking { SettingsDataStore.settings().selectedModelId })
    }

    @Test
    fun skipAdvancesEachStepWithoutChangingConfiguration() {
        val beforeProvider = runBlocking { ProviderRepository.allProviders().single() }
        val beforeSettings = runBlocking { SettingsDataStore.settings() }
        var finished = false
        showOnboarding { finished = true }

        composeRule.onNodeWithText(text(R.string.onboarding_skip)).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_step_connection)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_skip)).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_step_model)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_skip)).performClick()
        composeRule.onNodeWithText(text(R.string.onboarding_step_permissions)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_skip)).performClick()

        composeRule.waitUntil(5_000) { finished }
        assertTrue(runBlocking { SettingsDataStore.onboardingCompleted() })
        assertEquals(beforeProvider, runBlocking { ProviderRepository.providerById(beforeProvider.id) })
        val afterSettings = runBlocking { SettingsDataStore.settings() }
        assertEquals(beforeSettings.selectedProviderId, afterSettings.selectedProviderId)
        assertEquals(beforeSettings.selectedModelId, afterSettings.selectedModelId)
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

    private fun showOnboarding(onFinished: () -> Unit = {}) {
        composeRule.setContent {
            AgentAppTheme(appearance = AppearanceSettings(), applyInterfaceScale = false) {
                OnboardingScreen(onFinished = onFinished)
            }
        }
        composeRule.waitForIdle()
    }

    private fun navigateToModel() {
        next()
        composeRule.onNodeWithText(text(R.string.onboarding_step_connection)).assertIsDisplayed()
        next()
        waitForText("Original model")
    }

    private fun next() {
        composeRule.onNodeWithText(text(R.string.onboarding_next)).assertIsEnabled().performClick()
    }

    private fun waitForText(value: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
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

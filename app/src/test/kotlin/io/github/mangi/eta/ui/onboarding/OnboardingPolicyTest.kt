package io.github.mangi.eta.ui.onboarding

import io.github.mangi.eta.data.model.CustomProviderSetting
import io.github.mangi.eta.data.model.Model
import io.github.mangi.eta.data.model.ModelSource
import io.github.mangi.eta.data.model.OpenAiCompatibleProviderSetting
import io.github.mangi.eta.data.model.OpenAiEndpointMode
import io.github.mangi.eta.data.model.ProviderAuthModes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPolicyTest {
    @Test
    fun freshInstallNeedsSetupUntilCompleted() {
        assertTrue(OnboardingPolicy.shouldShow(completed = false, providers = emptyList()))
        assertFalse(OnboardingPolicy.shouldShow(completed = true, providers = emptyList()))
        assertTrue(OnboardingPolicy.shouldShow(completed = false, providers = listOf(provider())))
    }

    @Test
    fun existingCredentialsAndCustomConnectionsSkipSetup() {
        assertFalse(OnboardingPolicy.shouldShow(false, listOf(provider(apiKey = "configured"))))
        assertFalse(OnboardingPolicy.shouldShow(false, listOf(provider(authMode = ProviderAuthModes.OPENAI_CHATGPT))))
        assertFalse(OnboardingPolicy.shouldShow(false, listOf(CustomProviderSetting(
            id = "custom",
            name = "Custom",
            baseUrl = "https://example.invalid",
            endpointMode = OpenAiEndpointMode.CHAT_COMPLETIONS,
        ))))
    }

    @Test
    fun existingManualOrRemoteModelsSkipButCatalogDoesNot() {
        assertFalse(OnboardingPolicy.shouldShow(false, listOf(provider(modelSource = ModelSource.MANUAL))))
        assertFalse(OnboardingPolicy.shouldShow(false, listOf(provider(modelSource = ModelSource.REMOTE))))
        assertTrue(OnboardingPolicy.shouldShow(false, listOf(provider(modelSource = ModelSource.CATALOG))))
    }

    private fun provider(
        apiKey: String = "",
        authMode: String = ProviderAuthModes.API_KEY,
        modelSource: ModelSource? = null,
    ) = OpenAiCompatibleProviderSetting(
        id = "builtin-test",
        name = "Test",
        baseUrl = "https://example.invalid",
        apiKey = apiKey,
        authMode = authMode,
        isBuiltIn = true,
        endpointMode = OpenAiEndpointMode.CHAT_COMPLETIONS,
        models = modelSource?.let {
            listOf(Model(id = "model", modelId = "model", displayName = "Model", source = it))
        }.orEmpty(),
    )
}

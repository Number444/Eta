package io.github.mangi.eta.ui.onboarding

import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.model.ModelSource
import io.github.mangi.eta.data.model.ProviderAuthModes
import io.github.mangi.eta.data.model.ProviderSetting
import io.github.mangi.eta.data.repository.ProviderRepository

internal object OnboardingPolicy {
    suspend fun shouldShow(): Boolean = shouldShow(
        completed = SettingsDataStore.onboardingCompleted(),
        providers = ProviderRepository.allProviders(),
    )

    internal fun shouldShow(completed: Boolean, providers: List<ProviderSetting>): Boolean {
        if (completed) return false
        return providers.none { provider ->
            provider.apiKey.isNotBlank() ||
                ProviderAuthModes.isOAuth(provider.authMode) ||
                !provider.isBuiltIn ||
                provider.models.any { it.source != ModelSource.CATALOG }
        }
    }
}

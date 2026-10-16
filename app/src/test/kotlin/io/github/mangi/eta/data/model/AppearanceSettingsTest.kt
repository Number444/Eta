package io.github.mangi.eta.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun defaultsPreserveExistingAppearanceBehavior() {
        val settings = AppearanceSettings()

        assertEquals(AppearanceThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.monetEnabled)
        assertEquals(AppearanceBlurMode.LIQUID_GLASS, settings.blurMode)
        assertTrue(settings.swipeDismissEnabled)
        assertTrue(settings.predictiveBackEnabled)
        assertEquals(DEFAULT_INTERFACE_SCALE, settings.interfaceScale)
    }

    @Test
    fun invalidPersistedEnumsFallBackToStableDefaults() {
        assertEquals(AppearanceThemeMode.SYSTEM, AppearanceThemeMode.fromPersistedValue("unknown"))
        assertEquals(AppearancePaletteStyle.TONAL_SPOT, AppearancePaletteStyle.fromPersistedValue(null))
        assertEquals(AppearanceAccentColor.SYSTEM, AppearanceAccentColor.fromPersistedValue(""))
    }

    @Test
    fun blurModePersistedValueRoundTrips() {
        AppearanceBlurMode.entries.forEach { mode ->
            assertEquals(mode, AppearanceBlurMode.fromPersistedValue(mode.persistedValue))
        }
        assertEquals(AppearanceBlurMode.LIQUID_GLASS, AppearanceBlurMode.fromPersistedValue(null))
        assertEquals(AppearanceBlurMode.LIQUID_GLASS, AppearanceBlurMode.fromPersistedValue("unknown"))
    }

    @Test
    fun legacyBlurEnabledMapsToBlurMode() {
        assertEquals(AppearanceBlurMode.NONE, AppearanceBlurMode.fromLegacyBlurEnabled(false))
        assertEquals(AppearanceBlurMode.LIQUID_GLASS, AppearanceBlurMode.fromLegacyBlurEnabled(true))
        assertEquals(AppearanceBlurMode.LIQUID_GLASS, AppearanceBlurMode.fromLegacyBlurEnabled(null))
    }

    @Test
    fun interfaceScaleIsFiniteAndClamped() {
        assertEquals(MIN_INTERFACE_SCALE, normalizeInterfaceScale(0.2f))
        assertEquals(MAX_INTERFACE_SCALE, normalizeInterfaceScale(2f))
        assertEquals(DEFAULT_INTERFACE_SCALE, normalizeInterfaceScale(Float.NaN))
        assertEquals(DEFAULT_INTERFACE_SCALE, normalizeInterfaceScale(Float.POSITIVE_INFINITY))
        assertEquals(0.95f, normalizeInterfaceScale(0.95f))
    }
}

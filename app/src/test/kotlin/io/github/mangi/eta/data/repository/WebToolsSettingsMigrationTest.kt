package io.github.mangi.eta.data.repository

import io.github.mangi.eta.data.model.WebToolBackend
import org.junit.Assert.assertEquals
import org.junit.Test

class WebToolsSettingsMigrationTest {
    @Test
    fun legacyDeepSeekSelectionMigratesToOfficialBackend() {
        assertEquals(
            WebToolBackend.DEEPSEEK,
            legacyModSearchBackend("deepseek", WebToolBackend.BUILTIN),
        )
    }

    @Test
    fun legacyExaSelectionFallsBackToBuiltin() {
        assertEquals(
            WebToolBackend.BUILTIN,
            legacyModSearchBackend("exa", WebToolBackend.CUSTOM),
        )
    }

    @Test
    fun missingLegacySelectionKeepsOfficialSettings() {
        assertEquals(
            WebToolBackend.CUSTOM,
            legacyModSearchBackend("", WebToolBackend.CUSTOM),
        )
    }
}

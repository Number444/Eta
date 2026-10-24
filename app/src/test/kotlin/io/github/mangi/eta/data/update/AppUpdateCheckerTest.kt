package io.github.mangi.eta.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun newerPatchVersionIsNewer() {
        assertTrue(AppUpdateChecker.isNewer("3.0.7", "3.0.6"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(AppUpdateChecker.isNewer("3.0.5", "3.0.6"))
    }

    @Test
    fun missingSegmentsCountAsZero() {
        assertTrue(AppUpdateChecker.isNewer("3.1", "3.0.9"))
        assertFalse(AppUpdateChecker.isNewer("3.0", "3.0.1"))
    }

    @Test
    fun vPrefixIsIgnored() {
        assertTrue(AppUpdateChecker.isNewer("v3.1.0", "3.0.6"))
        assertFalse(AppUpdateChecker.isNewer("v3.0.6", "3.0.6"))
    }

    @Test
    fun debugSuffixIsIgnored() {
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6-debug"))
        assertTrue(AppUpdateChecker.isNewer("3.0.10", "3.0.9-debug"))
    }

    @Test
    fun buildMetadataIsIgnored() {
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6+a1b2c3d"))
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6-debug+a1b2c3d"))
        assertTrue(AppUpdateChecker.isNewer("3.0.7", "3.0.6+a1b2c3d"))
    }

    // Eta Mod：本 fork 的 release tag 带 -NNN 修订号，基础版本相同时按修订号比较。
    @Test
    fun modRevisionIsComparedWhenBaseVersionsMatch() {
        assertTrue(AppUpdateChecker.isNewer("3.2.0-002", "3.2.0-001"))
        assertFalse(AppUpdateChecker.isNewer("3.2.0-001", "3.2.0-001"))
        assertFalse(AppUpdateChecker.isNewer("3.2.0-001", "3.2.0-002"))
        assertFalse(AppUpdateChecker.isNewer("3.2.0-001", "3.2.0-001-debug"))
        assertTrue(AppUpdateChecker.isNewer("v3.3.0-001", "3.2.0-009"))
        assertTrue(AppUpdateChecker.isNewer("3.2.0-001", "3.2.0"))
        assertFalse(AppUpdateChecker.isNewer("3.2.0", "3.2.0-001"))
    }

    @Test
    fun singleApkAssetBecomesDownloadUrl() {
        val release = AppUpdateChecker.parseLatest(
            """
            {
              "tag_name": "v3.3.3-002",
              "html_url": "https://github.com/Number444/Eta/releases/tag/v3.3.3-002",
              "assets": [
                {
                  "name": "EtaMod-3.3.3-002.apk",
                  "browser_download_url": "https://github.com/Number444/Eta/releases/download/v3.3.3-002/EtaMod-3.3.3-002.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("3.3.3-002", release.version)
        assertEquals(
            "https://github.com/Number444/Eta/releases/download/v3.3.3-002/EtaMod-3.3.3-002.apk",
            release.downloadUrl,
        )
    }

    @Test
    fun missingOrMultipleApkAssetsFallBackToReleasePage() {
        fun releasePageWithAssets(assets: String) = AppUpdateChecker.parseLatest(
            """
            {
              "tag_name": "v3.3.3-002",
              "html_url": "https://github.com/Number444/Eta/releases/tag/v3.3.3-002",
              "assets": [$assets]
            }
            """.trimIndent(),
        )

        assertEquals(releasePageWithAssets("").url, releasePageWithAssets("").downloadUrl)
        val apk =
            """{"name":"EtaMod.apk","browser_download_url":"https://example.com/EtaMod.apk"}"""
        assertEquals(
            releasePageWithAssets("$apk,$apk").url,
            releasePageWithAssets("$apk,$apk").downloadUrl,
        )
    }
}

package io.github.mangi.eta.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalShareImportPolicyTest {
    @Test
    fun supportedTypesReuseExistingAttachmentPaths() {
        assertEquals(ExternalShareImportKind.Image, ExternalShareImportPolicy.kindFor("image/jpeg"))
        assertEquals(ExternalShareImportKind.Image, ExternalShareImportPolicy.kindFor("image/jpg"))
        assertEquals(ExternalShareImportKind.Image, ExternalShareImportPolicy.kindFor("image/avif"))
        assertEquals(ExternalShareImportKind.File, ExternalShareImportPolicy.kindFor("text/plain"))
        assertEquals(ExternalShareImportKind.File, ExternalShareImportPolicy.kindFor("application/pdf"))
        assertEquals(
            ExternalShareImportKind.File,
            ExternalShareImportPolicy.kindFor(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ),
        )
        assertEquals(ExternalShareImportKind.File, ExternalShareImportPolicy.kindFor("application/zip"))
    }

    @Test
    fun octetStreamUsesOnlyKnownExtensions() {
        assertEquals(
            ExternalShareImportKind.Image,
            ExternalShareImportPolicy.kindFor("application/octet-stream", "photo.png"),
        )
        assertEquals(
            ExternalShareImportKind.File,
            ExternalShareImportPolicy.kindFor("application/octet-stream", "report.docx"),
        )
        assertEquals(
            ExternalShareImportKind.File,
            ExternalShareImportPolicy.kindFor(null, "notes.md"),
        )
        assertNull(ExternalShareImportPolicy.kindFor("application/octet-stream", "unknown.bin"))
    }

    @Test
    fun excludedMediaAndImageFormatsAreRejected() {
        listOf(
            "image/gif", "image/bmp", "image/tiff", "image/svg+xml",
            "image/vnd.microsoft.icon", "image/psd", "video/mp4", "audio/mpeg",
        ).forEach { mime ->
            assertNull(mime, ExternalShareImportPolicy.kindFor(mime))
        }
        listOf("bitmap.bmp", "vector.svg", "animation.gif", "movie.mp4", "voice.flac").forEach { name ->
            assertNull(name, ExternalShareImportPolicy.kindFor("application/octet-stream", name))
        }
    }
}

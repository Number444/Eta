package io.github.mangi.eta.ui

internal enum class ExternalShareImportKind { Image, File }

internal object ExternalShareImportPolicy {
    private val imageMimes = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif", "image/avif",
    )
    private val fileMimes = setOf(
        "application/json", "application/xml", "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/zip", "application/x-zip-compressed", "application/x-7z-compressed",
        "application/vnd.rar", "application/x-rar-compressed", "application/x-tar",
        "application/gzip", "application/x-gzip",
    )
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "avif")
    private val fileExtensions = setOf(
        "txt", "md", "markdown", "csv", "tsv", "json", "xml", "yaml", "yml", "log",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "7z", "rar", "tar", "gz",
    )
    private val excludedExtensions = setOf(
        "bmp", "tif", "tiff", "svg", "ico", "psd", "gif",
        "mp4", "mov", "avi", "mkv", "webm", "m4v", "3gp",
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "opus",
    )

    fun kindFor(mimeType: String?, displayName: String? = null): ExternalShareImportKind? {
        val mime = mimeType.orEmpty().substringBefore(';').trim().lowercase()
        if (mime.startsWith("video/") || mime.startsWith("audio/") || mime in excludedImageMimes) return null
        if (mime in imageMimes) return ExternalShareImportKind.Image
        if (mime.startsWith("text/") || mime in fileMimes) return ExternalShareImportKind.File
        if (mime.isNotEmpty() && mime != "application/octet-stream") return null

        val extension = displayName.orEmpty().substringAfterLast('.', "").lowercase()
        if (extension in excludedExtensions) return null
        return when (extension) {
            in imageExtensions -> ExternalShareImportKind.Image
            in fileExtensions -> ExternalShareImportKind.File
            else -> null
        }
    }

    private val excludedImageMimes = setOf(
        "image/gif", "image/bmp", "image/tiff", "image/svg+xml",
        "image/vnd.microsoft.icon", "image/x-icon", "image/psd", "image/vnd.adobe.photoshop",
    )
}

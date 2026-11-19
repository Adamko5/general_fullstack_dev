package dev.generalfullstack.filestorage.standard.api.dto

import java.net.URI
import java.time.Instant
import java.util.UUID

data class CreateImageUploadResponse(
    val imageId: UUID,
    val uploadUrl: URI,
    val requiredHeaders: Map<String, String>,
    val expiresAt: Instant,
)

data class CompleteImageUploadResponse(
    val imageId: UUID,
    val status: ImageStatus,
)

data class CreateImageDownloadResponse(
    val downloadUrl: URI,
    val expiresAt: Instant,
)

enum class ImageStatus {
    AVAILABLE,
}

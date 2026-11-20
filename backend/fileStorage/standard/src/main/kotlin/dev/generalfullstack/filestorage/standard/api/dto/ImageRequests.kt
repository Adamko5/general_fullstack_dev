package dev.generalfullstack.filestorage.standard.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive

const val MAX_IMAGE_SIZE_BYTES = 5_242_880L

data class CreateImageUploadRequest(
    val visibility: ImageVisibility,
    @field:Pattern(regexp = "image/(jpeg|png)")
    val contentType: String,
    @field:Positive
    @field:Max(MAX_IMAGE_SIZE_BYTES)
    val sizeBytes: Long,
)

enum class ImageVisibility {
    PUBLIC,
    PRIVATE,
}

data class ShareImageRequest(
    val recipientUserId: @jakarta.validation.constraints.NotNull java.util.UUID,
)

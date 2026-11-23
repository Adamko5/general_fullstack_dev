package dev.generalfullstack.filestorage.standard.api.error

import org.springframework.http.HttpStatus

class ImageApiException(
    val status: HttpStatus,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

package dev.generalfullstack.filestorage.webflux.api.error

import org.springframework.http.HttpStatus

class ImageApiException(
    val status: HttpStatus,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

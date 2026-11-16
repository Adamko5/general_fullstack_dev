package dev.generalfullstack.auth.api.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Standard API error response")
data class ApiErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val fieldErrors: Map<String, String> = emptyMap(),
)

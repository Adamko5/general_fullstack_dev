package dev.generalfullstack.auth.api.dto

data class CsrfTokenResponse(
    val headerName: String,
    val token: String,
)

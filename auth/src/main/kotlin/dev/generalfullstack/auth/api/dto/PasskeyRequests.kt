package dev.generalfullstack.auth.api.dto

import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.Email

data class PasskeyAuthenticationOptionsRequest(
    @field:Email
    val email: String? = null,
)

data class CompletePasskeyRegistrationRequest(
    val credential: JsonNode,
    val label: String? = null,
)

data class CompletePasskeyAuthenticationRequest(
    val credential: JsonNode,
)

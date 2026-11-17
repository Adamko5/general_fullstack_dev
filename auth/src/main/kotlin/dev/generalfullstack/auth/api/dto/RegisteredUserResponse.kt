package dev.generalfullstack.auth.api.dto

import java.util.UUID

data class RegisteredUserResponse(
    val id: UUID,
    val name: String,
    val email: String,
)

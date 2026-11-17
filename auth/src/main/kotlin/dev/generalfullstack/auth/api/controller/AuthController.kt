package dev.generalfullstack.auth.api.controller

import dev.generalfullstack.auth.api.dto.RegisterRequest
import dev.generalfullstack.auth.api.dto.RegisteredUserResponse
import dev.generalfullstack.auth.api.openapi.AuthApiOperation
import dev.generalfullstack.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @AuthApiOperation(
        summary = "Register an account",
        description = "Creates a user account using an email address, name, and password.",
    )
    fun register(@Valid @RequestBody request: RegisterRequest): RegisteredUserResponse {
        val account = authService.register(
            email = request.email,
            name = request.name,
            password = request.password,
        )
        return RegisteredUserResponse(
            id = account.id,
            name = account.name,
            email = account.email,
        )
    }
}

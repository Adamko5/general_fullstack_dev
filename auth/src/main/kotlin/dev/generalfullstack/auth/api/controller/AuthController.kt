package dev.generalfullstack.auth.api.controller

import dev.generalfullstack.auth.api.dto.LoginRequest
import dev.generalfullstack.auth.api.dto.LogoutRequest
import dev.generalfullstack.auth.api.dto.RefreshTokenRequest
import dev.generalfullstack.auth.api.dto.RegisterRequest
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
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Register an account",
        description = "Creates a user account using an email address, name, and password.",
    )
    fun register(@Valid @RequestBody request: RegisterRequest) {
        authService.register(
            email = request.email,
            name = request.name,
            password = request.password,
        )
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Log in with a password",
        description = "Authenticates a user and starts an auth session.",
    )
    fun login(@Valid @RequestBody request: LoginRequest) {
        authService.login(email = request.email, password = request.password)
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Refresh an access token",
        description = "Rotates a refresh token and issues a new access token.",
    )
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest) {
        authService.refresh(request.refreshToken)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Log out",
        description = "Revokes the session associated with the supplied refresh token.",
    )
    fun logout(@Valid @RequestBody request: LogoutRequest) {
        authService.logout(request.refreshToken)
    }
}

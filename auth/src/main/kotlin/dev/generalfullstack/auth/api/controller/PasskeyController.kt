package dev.generalfullstack.auth.api.controller

import dev.generalfullstack.auth.api.dto.CompletePasskeyAuthenticationRequest
import dev.generalfullstack.auth.api.dto.CompletePasskeyRegistrationRequest
import dev.generalfullstack.auth.api.dto.PasskeyAuthenticationOptionsRequest
import dev.generalfullstack.auth.api.openapi.AuthApiOperation
import dev.generalfullstack.auth.service.PasskeyService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/passkeys")
class PasskeyController(
    private val passkeyService: PasskeyService,
) {
    @PostMapping("/registration/options")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Start passkey registration",
        description = "Creates WebAuthn registration options for the authenticated user.",
        authenticated = true,
    )
    fun registrationOptions(authentication: Authentication) {
        passkeyService.createRegistrationOptions(authentication.name)
    }

    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Complete passkey registration",
        description = "Verifies and saves a newly registered passkey for the authenticated user.",
        authenticated = true,
    )
    fun completeRegistration(
        authentication: Authentication,
        @Valid @RequestBody request: CompletePasskeyRegistrationRequest,
    ) {
        passkeyService.completeRegistration(
            subject = authentication.name,
            credentialResponse = request.credential.toString(),
            label = request.label,
        )
    }

    @PostMapping("/authentication/options")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Start passkey authentication",
        description = "Creates WebAuthn authentication options for a passkey login attempt.",
    )
    fun authenticationOptions(
        @Valid @RequestBody request: PasskeyAuthenticationOptionsRequest,
    ) {
        passkeyService.createAuthenticationOptions(request.email)
    }

    @PostMapping("/authentication")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Complete passkey authentication",
        description = "Verifies a WebAuthn assertion and starts an auth session.",
    )
    fun completeAuthentication(
        @Valid @RequestBody request: CompletePasskeyAuthenticationRequest,
    ) {
        passkeyService.completeAuthentication(request.credential.toString())
    }

    @GetMapping
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "List registered passkeys",
        description = "Lists passkeys registered to the authenticated user.",
        authenticated = true,
    )
    fun listCredentials(authentication: Authentication) {
        passkeyService.listCredentials(authentication.name)
    }

    @DeleteMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @AuthApiOperation(
        summary = "Remove a passkey",
        description = "Removes one of the authenticated user's registered passkeys.",
        authenticated = true,
    )
    fun deleteCredential(
        authentication: Authentication,
        @PathVariable credentialId: String,
    ) {
        passkeyService.deleteCredential(authentication.name, credentialId)
    }
}

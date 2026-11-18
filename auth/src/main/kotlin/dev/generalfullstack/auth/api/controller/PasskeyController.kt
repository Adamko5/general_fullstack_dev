package dev.generalfullstack.auth.api.controller

import dev.generalfullstack.auth.api.openapi.AuthApiOperation
import dev.generalfullstack.auth.api.dto.PasskeyResponse
import dev.generalfullstack.auth.service.PasskeyService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/passkeys")
class PasskeyController(
    private val passkeyService: PasskeyService,
) {
    @GetMapping
    @AuthApiOperation(
        summary = "List registered passkeys",
        description = "Lists passkeys registered to the authenticated user.",
        authenticated = true,
    )
    fun listCredentials(authentication: Authentication): List<PasskeyResponse> =
        passkeyService.listCredentials(authentication.name)

    @DeleteMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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

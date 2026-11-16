package dev.generalfullstack.auth.service

import dev.generalfullstack.auth.repository.UserRepository
import dev.generalfullstack.auth.repository.WebAuthnChallengeRepository
import dev.generalfullstack.auth.repository.WebAuthnCredentialRepository
import org.springframework.stereotype.Service

@Service
class PasskeyService(
    private val userRepository: UserRepository,
    private val webAuthnCredentialRepository: WebAuthnCredentialRepository,
    private val webAuthnChallengeRepository: WebAuthnChallengeRepository,
) {
    fun createRegistrationOptions(subject: String) {
    }

    fun completeRegistration(subject: String, credentialResponse: String, label: String?) {
    }

    fun createAuthenticationOptions(email: String?) {
    }

    fun completeAuthentication(credentialResponse: String) {
    }

    fun listCredentials(subject: String) {
    }

    fun deleteCredential(subject: String, credentialId: String) {
    }
}

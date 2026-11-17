package dev.generalfullstack.auth.service

import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.stereotype.Service

@Service
class PasskeyService(
    private val userCredentialRepository: UserCredentialRepository,
    private val publicKeyCredentialUserEntityRepository: PublicKeyCredentialUserEntityRepository,
) {
    fun listCredentials(subject: String) {
    }

    fun deleteCredential(subject: String, credentialId: String) {
    }
}

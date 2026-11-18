package dev.generalfullstack.auth.service

import dev.generalfullstack.auth.api.dto.PasskeyResponse
import dev.generalfullstack.auth.api.error.ApiException
import org.springframework.http.HttpStatus
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.stereotype.Service

@Service
class PasskeyService(
    private val userCredentialRepository: UserCredentialRepository,
    private val publicKeyCredentialUserEntityRepository: PublicKeyCredentialUserEntityRepository,
) {
    fun listCredentials(username: String): List<PasskeyResponse> {
        val userEntity = publicKeyCredentialUserEntityRepository.findByUsername(username)
            ?: return emptyList()

        return userCredentialRepository.findByUserId(userEntity.id).map { credential ->
            PasskeyResponse(
                credentialId = credential.credentialId.toBase64UrlString(),
                label = credential.label,
                createdAt = credential.created,
                lastUsedAt = credential.lastUsed,
            )
        }
    }

    fun deleteCredential(username: String, credentialId: String) {
        val userEntity = publicKeyCredentialUserEntityRepository.findByUsername(username)
            ?: throw credentialNotFoundException()
        val credentialIdBytes = try {
            Bytes.fromBase64(credentialId)
        } catch (exception: IllegalArgumentException) {
            throw ApiException(
                status = HttpStatus.BAD_REQUEST,
                code = "invalid_credential_id",
                message = "The credential ID is invalid.",
            )
        }
        val credential = userCredentialRepository.findByCredentialId(credentialIdBytes)
            ?: throw credentialNotFoundException()

        if (credential.userEntityUserId != userEntity.id) {
            throw credentialNotFoundException()
        }

        userCredentialRepository.delete(credentialIdBytes)
    }

    private fun credentialNotFoundException() = ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "passkey_not_found",
        message = "The passkey was not found.",
    )
}

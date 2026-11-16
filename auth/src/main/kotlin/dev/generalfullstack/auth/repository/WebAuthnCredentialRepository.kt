package dev.generalfullstack.auth.repository

import dev.generalfullstack.auth.persistence.entity.WebAuthnCredentialEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WebAuthnCredentialRepository : JpaRepository<WebAuthnCredentialEntity, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<WebAuthnCredentialEntity>
    fun findByCredentialId(credentialId: ByteArray): WebAuthnCredentialEntity?
    fun deleteByCredentialIdAndUserId(credentialId: ByteArray, userId: UUID)
}

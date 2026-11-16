package dev.generalfullstack.auth.repository

import dev.generalfullstack.auth.persistence.entity.WebAuthnChallengeEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WebAuthnChallengeRepository : JpaRepository<WebAuthnChallengeEntity, UUID> {
    fun findByChallengeAndConsumedAtIsNull(challenge: ByteArray): WebAuthnChallengeEntity?
}

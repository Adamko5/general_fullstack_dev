package dev.generalfullstack.auth.repository

import dev.generalfullstack.auth.persistence.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: ByteArray): RefreshTokenEntity?
}

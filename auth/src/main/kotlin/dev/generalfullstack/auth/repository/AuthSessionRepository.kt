package dev.generalfullstack.auth.repository

import dev.generalfullstack.auth.persistence.entity.AuthSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuthSessionRepository : JpaRepository<AuthSessionEntity, UUID> {
    fun findAllByUserIdAndRevokedAtIsNull(userId: UUID): List<AuthSessionEntity>
}

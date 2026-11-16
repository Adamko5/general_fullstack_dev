package dev.generalfullstack.auth.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_sessions")
open class AuthSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null

    @Column(name = "user_id", nullable = false)
    open lateinit var userId: UUID

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant = Instant.now()

    @Column(name = "last_refresh_at", nullable = false)
    open var lastRefreshAt: Instant = Instant.now()

    @Column(name = "expires_at", nullable = false)
    open lateinit var expiresAt: Instant

    @Column(name = "revoked_at")
    open var revokedAt: Instant? = null

    @PrePersist
    open fun setCreationTimestamps() {
        val now = Instant.now()
        createdAt = now
        lastRefreshAt = now
    }
}

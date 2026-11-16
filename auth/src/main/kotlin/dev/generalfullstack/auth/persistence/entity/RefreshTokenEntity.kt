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
@Table(name = "refresh_tokens")
open class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null

    @Column(name = "session_id", nullable = false)
    open lateinit var sessionId: UUID

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "bytea")
    open lateinit var tokenHash: ByteArray

    @Column(name = "issued_at", nullable = false, updatable = false)
    open var issuedAt: Instant = Instant.now()

    @Column(name = "expires_at", nullable = false)
    open lateinit var expiresAt: Instant

    @Column(name = "consumed_at")
    open var consumedAt: Instant? = null

    @Column(name = "replaced_by_token_id")
    open var replacedByTokenId: UUID? = null

    @PrePersist
    open fun setIssuedAt() {
        issuedAt = Instant.now()
    }
}

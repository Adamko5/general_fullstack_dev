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
@Table(name = "webauthn_challenges")
open class WebAuthnChallengeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null

    @Column(nullable = false, unique = true, columnDefinition = "bytea")
    open lateinit var challenge: ByteArray

    @Column(nullable = false)
    open var purpose: String = "authentication"

    @Column(name = "user_id")
    open var userId: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant = Instant.now()

    @Column(name = "expires_at", nullable = false)
    open lateinit var expiresAt: Instant

    @Column(name = "consumed_at")
    open var consumedAt: Instant? = null

    @PrePersist
    open fun setCreatedAt() {
        createdAt = Instant.now()
    }
}

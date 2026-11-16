package dev.generalfullstack.auth.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "webauthn_credentials")
open class WebAuthnCredentialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null

    @Column(name = "user_id", nullable = false)
    open lateinit var userId: UUID

    @Column(name = "credential_id", nullable = false, unique = true, columnDefinition = "bytea")
    open lateinit var credentialId: ByteArray

    @Column(name = "public_key", nullable = false, columnDefinition = "bytea")
    open lateinit var publicKey: ByteArray

    @Column(name = "sign_count", nullable = false)
    open var signCount: Long = 0

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "transports", nullable = false, columnDefinition = "text[]")
    open var transports: Array<String> = emptyArray()

    @Column(nullable = false)
    open var label: String = "Passkey"

    @Column
    open var aaguid: UUID? = null

    @Column(name = "backup_eligible", nullable = false)
    open var backupEligible: Boolean = false

    @Column(name = "backup_state", nullable = false)
    open var backupState: Boolean = false

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant = Instant.now()

    @Column(name = "last_used_at")
    open var lastUsedAt: Instant? = null

    @PrePersist
    open fun setCreatedAt() {
        createdAt = Instant.now()
    }
}

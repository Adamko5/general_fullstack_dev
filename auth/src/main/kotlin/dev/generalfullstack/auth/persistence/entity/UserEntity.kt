package dev.generalfullstack.auth.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
open class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null

    @Column(nullable = false)
    open lateinit var name: String

    @Column(nullable = false)
    open lateinit var email: String

    @Column(name = "password_hash")
    open var passwordHash: String? = null

    @Column(name = "email_verified_at")
    open var emailVerifiedAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: Instant = Instant.now()

    @Column(name = "disabled_at")
    open var disabledAt: Instant? = null

    @PrePersist
    open fun setCreationTimestamps() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    open fun setUpdatedTimestamp() {
        updatedAt = Instant.now()
    }
}

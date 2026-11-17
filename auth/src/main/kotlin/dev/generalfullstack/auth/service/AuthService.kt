package dev.generalfullstack.auth.service

import dev.generalfullstack.auth.repository.UserRepository
import dev.generalfullstack.auth.api.error.ApiException
import dev.generalfullstack.auth.persistence.entity.UserEntity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun register(email: String, name: String, password: String): RegisteredAccount {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw duplicateEmailException()
        }

        val user = UserEntity().apply {
            this.name = name.trim()
            this.email = normalizedEmail
            this.passwordHash = passwordEncoder.encode(password)
        }

        val savedUser = try {
            userRepository.saveAndFlush(user)
        } catch (exception: DataIntegrityViolationException) {
            throw duplicateEmailException()
        }

        return RegisteredAccount(
            id = requireNotNull(savedUser.id),
            name = savedUser.name,
            email = savedUser.email,
        )
    }

    private fun duplicateEmailException() = ApiException(
        status = HttpStatus.CONFLICT,
        code = "email_already_registered",
        message = "An account with this email already exists.",
    )
}

data class RegisteredAccount(
    val id: UUID,
    val name: String,
    val email: String,
)

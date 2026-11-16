package dev.generalfullstack.auth.service

import dev.generalfullstack.auth.repository.AuthSessionRepository
import dev.generalfullstack.auth.repository.RefreshTokenRepository
import dev.generalfullstack.auth.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val authSessionRepository: AuthSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    fun register(email: String, name: String, password: String) {
    }

    fun login(email: String, password: String) {
    }

    fun refresh(refreshToken: String) {
    }

    fun logout(refreshToken: String) {
    }
}

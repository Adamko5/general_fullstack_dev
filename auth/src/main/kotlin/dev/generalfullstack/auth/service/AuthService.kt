package dev.generalfullstack.auth.service

import dev.generalfullstack.auth.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
) {
    fun register(email: String, name: String, password: String) {
    }
}

package dev.generalfullstack.auth.security

import dev.generalfullstack.auth.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class DatabaseUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmailIgnoreCase(username.trim())
            ?: throw UsernameNotFoundException("Invalid username or password")
        val userId = user.id
            ?: throw UsernameNotFoundException("Invalid username or password")
        val passwordHash = user.passwordHash
            ?: throw UsernameNotFoundException("Invalid username or password")

        return User.withUsername(user.email)
            .password(passwordHash)
            .disabled(user.disabledAt != null)
            .authorities(emptyList<GrantedAuthority>())
            .build()
    }
}

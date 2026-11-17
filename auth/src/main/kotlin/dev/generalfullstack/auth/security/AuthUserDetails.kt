package dev.generalfullstack.auth.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class AuthUserDetails(
    val userId: UUID,
    encodedPassword: String,
    private val enabled: Boolean,
) : UserDetails, CredentialsContainer {
    private var encodedPassword: String? = encodedPassword

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getPassword(): String? = encodedPassword

    override fun getUsername(): String = userId.toString()

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabled

    override fun eraseCredentials() {
        encodedPassword = null
    }
}

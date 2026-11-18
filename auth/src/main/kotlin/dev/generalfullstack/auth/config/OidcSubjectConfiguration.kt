package dev.generalfullstack.auth.config

import dev.generalfullstack.auth.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import java.util.UUID

@Configuration
class OidcSubjectConfiguration {
    @Bean
    fun stableUserSubjectCustomizer(
        userRepository: UserRepository,
    ): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            val principal = context.getPrincipal<Authentication>()
            val tokenType = context.tokenType.value
            val storedPrincipalName = context.authorization?.principalName ?: (principal?.name ?: "")
            val userId = resolveUserId(storedPrincipalName, userRepository)

            if (userId != null &&
                (tokenType == OAuth2TokenType.ACCESS_TOKEN.value || tokenType == OidcParameterNames.ID_TOKEN)
            ) {
                context.claims.subject(userId.toString())
            }
        }

    private fun resolveUserId(principalName: String, userRepository: UserRepository): UUID? =
        try {
            UUID.fromString(principalName)
        } catch (exception: IllegalArgumentException) {
            userRepository.findByEmailIgnoreCase(principalName)?.id
        }
}

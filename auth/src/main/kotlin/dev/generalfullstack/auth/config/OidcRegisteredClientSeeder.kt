package dev.generalfullstack.auth.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.util.UUID

@Configuration
class OidcRegisteredClientSeeder {
    @Bean
    fun seedDevelopmentOidcClient(
        registeredClientRepository: RegisteredClientRepository,
        properties: OidcProperties,
    ): ApplicationRunner = ApplicationRunner {
        if (registeredClientRepository.findByClientId(properties.developmentClientId) == null) {
            val client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(properties.developmentClientId)
                .clientName("Local Next.js client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(properties.developmentClientRedirectUri)
                .postLogoutRedirectUri(properties.developmentClientPostLogoutRedirectUri)
                .scope(OidcScopes.OPENID)
                .clientSettings(
                    ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build(),
                )
                .tokenSettings(
                    TokenSettings.builder()
                        .accessTokenTimeToLive(properties.accessTokenTtl)
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .refreshTokenTimeToLive(properties.refreshTokenTtl)
                        .reuseRefreshTokens(false)
                        .build(),
                )
                .build()

            registeredClientRepository.save(client)
        }
    }
}

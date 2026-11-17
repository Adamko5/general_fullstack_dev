package dev.generalfullstack.auth.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings

@Configuration
@EnableConfigurationProperties(OidcProperties::class)
class OidcAuthorizationServerConfiguration {
    @Bean
    fun authorizationServerSettings(properties: OidcProperties): AuthorizationServerSettings =
        AuthorizationServerSettings.builder()
            .issuer(properties.issuer)
            .build()

    @Bean
    fun registeredClientRepository(jdbcOperations: JdbcOperations): RegisteredClientRepository =
        JdbcRegisteredClientRepository(jdbcOperations)

    @Bean
    fun oauth2AuthorizationService(
        jdbcOperations: JdbcOperations,
        registeredClientRepository: RegisteredClientRepository,
    ): OAuth2AuthorizationService =
        JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository)

    @Bean
    fun oauth2AuthorizationConsentService(
        jdbcOperations: JdbcOperations,
        registeredClientRepository: RegisteredClientRepository,
    ): OAuth2AuthorizationConsentService =
        JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository)
}

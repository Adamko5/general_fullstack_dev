package dev.generalfullstack.auth.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.util.UUID

@Configuration
class OidcRegisteredClientSeeder {
    @Bean
    fun seedConfiguredOidcClients(
        registeredClientRepository: RegisteredClientRepository,
        passwordEncoder: PasswordEncoder,
        properties: OidcProperties,
        resourceLoader: ResourceLoader,
        objectMapper: ObjectMapper,
    ): ApplicationRunner = ApplicationRunner {
        val registry = resourceLoader.getResource(properties.clientsConfigLocation)
            .inputStream
            .use { objectMapper.readValue(it, OidcClientRegistry::class.java) }

        require(registry.clients.map { it.clientId }.distinct().size == registry.clients.size) {
            "OIDC client configuration contains duplicate client IDs"
        }

        registry.clients.forEach { definition ->
            if (registeredClientRepository.findByClientId(definition.clientId) == null) {
                registeredClientRepository.save(definition.toRegisteredClient(passwordEncoder))
            }
        }
    }
}

private fun OidcClientDefinition.toRegisteredClient(passwordEncoder: PasswordEncoder): RegisteredClient {
    val authenticationMethods = clientAuthenticationMethods
        .map(ClientAuthenticationMethod::valueOf)
        .toSet()

    require(authenticationMethods.isNotEmpty()) { "Client '$clientId' must have an authentication method" }

    val secretAuthenticationMethods = setOf(
        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
        ClientAuthenticationMethod.CLIENT_SECRET_POST,
        ClientAuthenticationMethod.CLIENT_SECRET_JWT,
    )
    val requiresSecret = authenticationMethods.any(secretAuthenticationMethods::contains)
    require(requiresSecret == !clientSecret.isNullOrBlank()) {
        "Client '$clientId' must provide a plaintext secret exactly when using a client-secret authentication method"
    }

    if (authenticationMethods == setOf(ClientAuthenticationMethod.NONE)) {
        require(clientSettings.requireProofKey) {
            "Public client '$clientId' must require PKCE"
        }
    }

    val registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId(clientId)
        .clientName(clientName)
        .clientAuthenticationMethods { it.addAll(authenticationMethods) }
        .authorizationGrantTypes { grantTypes ->
            authorizationGrantTypes.mapTo(grantTypes, ::AuthorizationGrantType)
        }
        .redirectUris { it.addAll(redirectUris) }
        .postLogoutRedirectUris { it.addAll(postLogoutRedirectUris) }
        .scopes { it.addAll(scopes) }
        .clientSettings(clientSettings.toClientSettings())
        .tokenSettings(tokenSettings.toTokenSettings())

    clientIdIssuedAt?.let(registeredClient::clientIdIssuedAt)
    clientSecret?.let { registeredClient.clientSecret(requireNotNull(passwordEncoder.encode(it))) }
    clientSecretExpiresAt?.let(registeredClient::clientSecretExpiresAt)

    return registeredClient.build()
}

private fun OidcClientSettings.toClientSettings(): ClientSettings =
    ClientSettings.builder()
        .requireProofKey(requireProofKey)
        .requireAuthorizationConsent(requireAuthorizationConsent)
        .apply {
            jwkSetUrl?.let { jwkSetUrl(it) }
            tokenEndpointAuthenticationSigningAlgorithm?.let {
                tokenEndpointAuthenticationSigningAlgorithm(it.toJwsAlgorithm())
            }
            x509CertificateSubjectDN?.let { x509CertificateSubjectDN(it) }
        }
        .build()

private fun OidcTokenSettings.toTokenSettings(): TokenSettings =
    TokenSettings.builder()
        .authorizationCodeTimeToLive(authorizationCodeTimeToLive)
        .accessTokenTimeToLive(accessTokenTimeToLive)
        .accessTokenFormat(accessTokenFormat.toOAuth2TokenFormat())
        .deviceCodeTimeToLive(deviceCodeTimeToLive)
        .reuseRefreshTokens(reuseRefreshTokens)
        .refreshTokenTimeToLive(refreshTokenTimeToLive)
        .idTokenSignatureAlgorithm(SignatureAlgorithm.valueOf(idTokenSignatureAlgorithm))
        .x509CertificateBoundAccessTokens(x509CertificateBoundAccessTokens)
        .build()

private fun String.toJwsAlgorithm(): JwsAlgorithm =
    SignatureAlgorithm.from(this)
        ?: MacAlgorithm.from(this)
        ?: error("Unsupported JWS algorithm '$this'")

private fun String.toOAuth2TokenFormat(): OAuth2TokenFormat =
    when (this) {
        "self-contained" -> OAuth2TokenFormat.SELF_CONTAINED
        "reference" -> OAuth2TokenFormat.REFERENCE
        else -> error("Unsupported OAuth2 access-token format '$this'")
    }

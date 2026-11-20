package dev.generalfullstack.auth.config

import java.time.Duration
import java.time.Instant

data class OidcClientRegistry(
    val clients: List<OidcClientDefinition>,
)

data class OidcClientDefinition(
    val clientId: String,
    val clientIdIssuedAt: Instant?,
    val clientSecret: String?,
    val clientSecretExpiresAt: Instant?,
    val clientName: String,
    val clientAuthenticationMethods: Set<String>,
    val authorizationGrantTypes: Set<String>,
    val redirectUris: Set<String>,
    val postLogoutRedirectUris: Set<String>,
    val scopes: Set<String>,
    val allowedResourceUris: Set<String>,
    val clientSettings: OidcClientSettings,
    val tokenSettings: OidcTokenSettings,
)

data class OidcClientSettings(
    val requireProofKey: Boolean,
    val requireAuthorizationConsent: Boolean,
    val jwkSetUrl: String?,
    val tokenEndpointAuthenticationSigningAlgorithm: String?,
    val x509CertificateSubjectDN: String?,
)

data class OidcTokenSettings(
    val authorizationCodeTimeToLive: Duration,
    val accessTokenTimeToLive: Duration,
    val accessTokenFormat: String,
    val deviceCodeTimeToLive: Duration,
    val reuseRefreshTokens: Boolean,
    val refreshTokenTimeToLive: Duration,
    val idTokenSignatureAlgorithm: String,
    val x509CertificateBoundAccessTokens: Boolean,
)

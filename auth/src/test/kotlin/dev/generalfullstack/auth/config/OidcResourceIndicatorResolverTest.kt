package dev.generalfullstack.auth.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration

class OidcResourceIndicatorResolverTest {
    private val fileApi = "http://localhost:8083"
    private val reportingApi = "http://localhost:8084"
    private val resolver = OidcResourceIndicatorResolver(
        OidcClientRegistry(
            clients = listOf(
                OidcClientDefinition(
                    clientId = "test-frontend",
                    clientIdIssuedAt = null,
                    clientSecret = null,
                    clientSecretExpiresAt = null,
                    clientName = "Test frontend",
                    clientAuthenticationMethods = setOf("none"),
                    authorizationGrantTypes = setOf("authorization_code", "refresh_token"),
                    redirectUris = setOf("http://localhost:3000/"),
                    postLogoutRedirectUris = setOf("http://localhost:3000/"),
                    scopes = setOf("openid"),
                    allowedResourceUris = setOf(fileApi, reportingApi),
                    clientSettings = OidcClientSettings(
                        requireProofKey = true,
                        requireAuthorizationConsent = false,
                        jwkSetUrl = null,
                        tokenEndpointAuthenticationSigningAlgorithm = null,
                        x509CertificateSubjectDN = null,
                    ),
                    tokenSettings = OidcTokenSettings(
                        authorizationCodeTimeToLive = Duration.ofMinutes(5),
                        accessTokenTimeToLive = Duration.ofMinutes(10),
                        accessTokenFormat = "self-contained",
                        deviceCodeTimeToLive = Duration.ofMinutes(5),
                        reuseRefreshTokens = false,
                        refreshTokenTimeToLive = Duration.ofDays(30),
                        idTokenSignatureAlgorithm = "RS256",
                        x509CertificateBoundAccessTokens = false,
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `resolves multiple resources allowed for the client`() {
        val resources = resolver.requestedResources(
            clientId = "test-frontend",
            additionalParameters = mapOf("resource" to arrayOf(fileApi, reportingApi)),
        )

        assertEquals(listOf(fileApi, reportingApi), resources)
    }

    @Test
    fun `rejects a resource not allowed for the client`() {
        assertThrows(InvalidResourceIndicatorException::class.java) {
            resolver.requestedResources(
                clientId = "test-frontend",
                additionalParameters = mapOf("resource" to "http://localhost:9999"),
            )
        }
    }

    @Test
    fun `allows authorization requests with no API resource`() {
        val resources = resolver.requestedResources(
            clientId = "test-frontend",
            additionalParameters = emptyMap<String, Any>(),
        )

        assertEquals(emptyList<String>(), resources)
    }
}

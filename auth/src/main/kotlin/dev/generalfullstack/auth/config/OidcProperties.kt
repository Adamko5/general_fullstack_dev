package dev.generalfullstack.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("auth.oidc")
data class OidcProperties(
    val issuer: String,
    val allowedOrigins: List<String>,
    val signingKeyId: String,
    val privateKeyLocation: String,
    val publicKeyLocation: String,
    val clientsConfigLocation: String,
)

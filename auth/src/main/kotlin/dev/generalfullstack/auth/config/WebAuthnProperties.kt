package dev.generalfullstack.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("auth.webauthn")
data class WebAuthnProperties(
    val rpName: String,
    val rpId: String,
    val allowedOrigins: Set<String>,
)

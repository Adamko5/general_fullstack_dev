package dev.generalfullstack.filestorage.standard.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("auth.oidc")
data class AuthProperties(
    val issuerUri: String,
    val audience: String,
)

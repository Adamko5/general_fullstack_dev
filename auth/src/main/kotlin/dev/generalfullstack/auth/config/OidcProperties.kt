package dev.generalfullstack.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("auth.oidc")
data class OidcProperties(
    var issuer: String = "http://localhost:8080",
    var developmentClientId: String = "local-nextjs",
    var developmentClientRedirectUri: String = "http://localhost:3000/oidc/callback",
    var developmentClientPostLogoutRedirectUri: String = "http://localhost:3000/",
    var accessTokenTtl: Duration = Duration.ofMinutes(10),
    var refreshTokenTtl: Duration = Duration.ofDays(30),
    var allowedOrigins: List<String> = listOf("http://localhost:3000"),
    var signingKeyId: String = "local-rsa-1",
    var privateKeyLocation: String = "file:./secrets/jwt-private.pem",
    var publicKeyLocation: String = "file:./secrets/jwt-public.pem",
)

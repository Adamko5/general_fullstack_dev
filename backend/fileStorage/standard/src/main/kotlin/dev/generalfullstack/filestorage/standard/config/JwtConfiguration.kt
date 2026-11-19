package dev.generalfullstack.filestorage.standard.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@Configuration
@EnableConfigurationProperties(AuthProperties::class)
class JwtConfiguration {
    @Bean
    fun jwtDecoder(properties: AuthProperties): JwtDecoder {
        val decoder = NimbusJwtDecoder.withIssuerLocation(properties.issuerUri).build()
        val audienceValidator = OAuth2TokenValidator<Jwt> { jwt ->
            if (jwt.audience.orEmpty().contains(properties.audience)) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "The token is not intended for this API.", null),
                )
            }
        }

        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(properties.issuerUri),
                audienceValidator,
            ),
        )

        return decoder
    }
}

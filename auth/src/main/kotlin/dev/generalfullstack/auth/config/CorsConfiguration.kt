package dev.generalfullstack.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class OidcCorsConfiguration {
    @Bean
    fun corsConfigurationSource(properties: OidcProperties): CorsConfigurationSource {
        val cors = org.springframework.web.cors.CorsConfiguration().apply {
            allowedOrigins = properties.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf(
                "Accept",
                "Authorization",
                "Content-Type",
                "X-CSRF-TOKEN",
                "X-XSRF-TOKEN",
            )
            allowCredentials = true
            maxAge = 3600
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", cors)
        }
    }
}

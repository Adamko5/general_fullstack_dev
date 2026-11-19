package dev.generalfullstack.filestorage.standard.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfiguration {
    @Bean
    fun corsConfigurationSource(
        @Value("\${cors.allowed-origins}") allowedOrigins: List<String>,
    ): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            this.allowedOrigins = allowedOrigins
            allowedMethods = listOf("GET", "POST", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Accept", "Authorization", "Content-Type")
            allowCredentials = false
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", cors)
        }
    }

    @Bean
    fun imageApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { requests ->
                requests.requestMatchers(HttpMethod.GET, "/images/*/download").permitAll()
                requests.anyRequest().authenticated()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt(Customizer.withDefaults())
            }

        return http.build()
    }
}

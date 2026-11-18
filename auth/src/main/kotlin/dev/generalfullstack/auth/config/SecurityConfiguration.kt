package dev.generalfullstack.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableConfigurationProperties(WebAuthnProperties::class)
class SecurityConfiguration {
    @Bean
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder,
        webAuthnProperties: WebAuthnProperties,
    ): SecurityFilterChain {
        http.authorizeHttpRequests { requests ->
            requests.requestMatchers(
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/webjars/**",
                "/actuator/health",
                "/login",
                "/login/**",
                "/webauthn/authenticate/**",
                "/api/v1/auth/csrf",
            ).permitAll()
            requests.requestMatchers("/api/v1/auth/register").permitAll()
            requests.anyRequest().authenticated()
        }
        val passwordAuthenticationProvider = DaoAuthenticationProvider(userDetailsService)
        passwordAuthenticationProvider.setPasswordEncoder(passwordEncoder)
        http.authenticationProvider(passwordAuthenticationProvider)
        http.cors(Customizer.withDefaults())
        http.formLogin(Customizer.withDefaults())
        http.webAuthn { webAuthn ->
            webAuthn.rpName(webAuthnProperties.rpName)
            webAuthn.rpId(webAuthnProperties.rpId)
            webAuthn.allowedOrigins(webAuthnProperties.allowedOrigins)
        }

        return http.build()
    }
}

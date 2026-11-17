package dev.generalfullstack.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher

@Configuration
class AuthorizationServerSecurityConfiguration {
    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.oauth2AuthorizationServer { authorizationServer ->
            http.securityMatcher(authorizationServer.endpointsMatcher)
            authorizationServer.oidc(Customizer.withDefaults())
        }
        http.authorizeHttpRequests { requests -> requests.anyRequest().authenticated() }
        http.exceptionHandling { exceptions ->
            exceptions.defaultAuthenticationEntryPointFor(
                LoginUrlAuthenticationEntryPoint("/login"),
                MediaTypeRequestMatcher(MediaType.TEXT_HTML),
            )
        }
        http.cors(Customizer.withDefaults())
        http.oauth2ResourceServer { resourceServer ->
            resourceServer.jwt(Customizer.withDefaults())
        }

        return http.build()
    }
}


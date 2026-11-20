package dev.generalfullstack.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher

@Configuration
class AuthorizationServerSecurityConfiguration {
    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(
        http: HttpSecurity,
        resourceIndicatorResolver: OidcResourceIndicatorResolver,
    ): SecurityFilterChain {
        http.oauth2AuthorizationServer { authorizationServer ->
            http.securityMatcher(authorizationServer.endpointsMatcher)
            authorizationServer.oidc(Customizer.withDefaults())
            authorizationServer.authorizationEndpoint { endpoint ->
                endpoint.authenticationProviders { providers ->
                    providers.filterIsInstance<OAuth2AuthorizationCodeRequestAuthenticationProvider>()
                        .forEach { provider ->
                            val defaultValidator = OAuth2AuthorizationCodeRequestAuthenticationValidator()
                            provider.setAuthenticationValidator { context ->
                                defaultValidator.accept(context)
                                try {
                                    resourceIndicatorResolver.requestedResources(
                                        context.registeredClient.clientId,
                                        context.getAuthentication<OAuth2AuthorizationCodeRequestAuthenticationToken>()
                                            .additionalParameters,
                                    )
                                } catch (exception: InvalidResourceIndicatorException) {
                                    throw OAuth2AuthorizationCodeRequestAuthenticationException(
                                        OAuth2Error("invalid_target", exception.message, null),
                                        context.getAuthentication(),
                                    )
                                }
                            }
                        }
                }
            }
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


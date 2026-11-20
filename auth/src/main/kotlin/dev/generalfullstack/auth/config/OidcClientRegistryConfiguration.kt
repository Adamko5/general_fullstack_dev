package dev.generalfullstack.auth.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import java.net.URI

@Configuration
@EnableConfigurationProperties(OidcProperties::class)
class OidcClientRegistryConfiguration {
    @Bean
    fun oidcClientRegistry(
        properties: OidcProperties,
        resourceLoader: ResourceLoader,
        objectMapper: ObjectMapper,
    ): OidcClientRegistry {
        val registry = resourceLoader.getResource(properties.clientsConfigLocation)
            .inputStream
            .use { objectMapper.readValue(it, OidcClientRegistry::class.java) }

        require(registry.clients.map { it.clientId }.distinct().size == registry.clients.size) {
            "OIDC client configuration contains duplicate client IDs"
        }
        registry.clients.forEach { client ->
            require(client.allowedResourceUris.all(::isValidResourceUri)) {
                "Client '${client.clientId}' has an invalid allowed resource URI"
            }
        }

        return registry
    }

    private fun isValidResourceUri(value: String): Boolean =
        runCatching { URI(value) }
            .getOrNull()
            ?.let { it.isAbsolute && it.fragment == null }
            ?: false
}

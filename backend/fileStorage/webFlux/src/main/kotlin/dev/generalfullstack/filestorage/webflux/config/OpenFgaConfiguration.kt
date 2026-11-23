package dev.generalfullstack.filestorage.webflux.config

import dev.openfga.sdk.api.client.OpenFgaClient
import dev.openfga.sdk.api.configuration.ClientConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OpenFgaProperties::class)
class OpenFgaConfiguration {
    @Bean
    fun openFgaClient(properties: OpenFgaProperties): OpenFgaClient =
        OpenFgaClient(
            ClientConfiguration()
                .apiUrl(properties.apiUrl.toString())
                .storeId(properties.storeId)
                .authorizationModelId(properties.authorizationModelId),
        )
}

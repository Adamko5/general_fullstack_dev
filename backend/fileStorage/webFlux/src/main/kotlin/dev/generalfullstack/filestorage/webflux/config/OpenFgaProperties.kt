package dev.generalfullstack.filestorage.webflux.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties("openfga")
data class OpenFgaProperties(
    val apiUrl: URI,
    val storeId: String,
    val authorizationModelId: String,
)

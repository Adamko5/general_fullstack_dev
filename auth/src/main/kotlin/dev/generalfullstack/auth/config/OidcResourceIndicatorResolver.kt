package dev.generalfullstack.auth.config

import org.springframework.stereotype.Component
import java.net.URI

@Component
class OidcResourceIndicatorResolver(
    private val clientRegistry: OidcClientRegistry,
) {
    fun requestedResources(clientId: String, additionalParameters: Map<String, *>): List<String> {
        val resourceParameter = additionalParameters[RESOURCE_PARAMETER] ?: return emptyList()
        val resources = when (resourceParameter) {
            is String -> listOf(resourceParameter)
            is Array<*> -> resourceParameter.map { it as? String ?: throw invalidResource() }
            else -> throw invalidResource()
        }

        val allowedResources = clientRegistry.clients
            .firstOrNull { it.clientId == clientId }
            ?.allowedResourceUris
            .orEmpty()

        if (resources.isEmpty() || resources.distinct().size != resources.size ||
            resources.any { it !in allowedResources || !isValidResourceUri(it) }
        ) {
            throw invalidResource()
        }

        return resources
    }

    private fun isValidResourceUri(value: String): Boolean =
        runCatching { URI(value) }
            .getOrNull()
            ?.let { it.isAbsolute && it.fragment == null }
            ?: false

    private fun invalidResource() =
        InvalidResourceIndicatorException("The requested resource is not allowed for this client.")

    private companion object {
        const val RESOURCE_PARAMETER = "resource"
    }
}

class InvalidResourceIndicatorException(message: String) : RuntimeException(message)

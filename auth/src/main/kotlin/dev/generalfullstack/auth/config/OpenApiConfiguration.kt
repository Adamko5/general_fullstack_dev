package dev.generalfullstack.auth.config

import dev.generalfullstack.auth.api.openapi.AuthApiOperation
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun authOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Auth API")
                .version("v1")
                .description("Authentication, session, and passkey endpoints."),
        )
        .tags(listOf(Tag().name("Authentication").description("Account and passkey authentication.")))
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"),
            ),
        )

    @Bean
    fun authOperationCustomizer(): OperationCustomizer = OperationCustomizer { operation, handlerMethod ->
        val endpoint = handlerMethod.getMethodAnnotation(AuthApiOperation::class.java)
        if (endpoint != null) {
            operation.summary = endpoint.summary
            if (endpoint.description.isNotBlank()) {
                operation.description = endpoint.description
            }
            operation.addTagsItem("Authentication")

            if (endpoint.authenticated) {
                operation.addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            }

            val responses = operation.responses ?: ApiResponses()
            responses.remove("200")
            responses.addApiResponse("400", ApiResponse().description("The request is invalid."))
            responses.addApiResponse("401", ApiResponse().description("Authentication is required."))
            responses.addApiResponse("403", ApiResponse().description("The request is forbidden."))
            responses.addApiResponse("500", ApiResponse().description("An unexpected error occurred."))
            responses.addApiResponse("501", ApiResponse().description("The endpoint is scaffolded but not implemented yet."))
            operation.responses = responses
        }
        operation
    }
}

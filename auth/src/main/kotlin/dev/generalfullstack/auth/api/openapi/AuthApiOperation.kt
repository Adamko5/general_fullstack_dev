package dev.generalfullstack.auth.api.openapi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthApiOperation(
    val summary: String,
    val description: String = "",
    val authenticated: Boolean = false,
    val notImplemented: Boolean = false,
)

package dev.generalfullstack.filestorage.webflux.api.error

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import java.net.URI

@RestControllerAdvice
class ImageApiExceptionHandler {
    @ExceptionHandler(ImageApiException::class)
    fun handleImageApiException(exception: ImageApiException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(
            exception.status,
            exception.message ?: "The image request could not be completed.",
        )
        problem.type = URI.create("about:blank")
        problem.title = exception.status.reasonPhrase

        return ResponseEntity.status(exception.status).body(problem)
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(exception: WebExchangeBindException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "The request contains invalid fields.",
        )
        problem.type = URI.create("about:blank")
        problem.title = HttpStatus.BAD_REQUEST.reasonPhrase
        problem.setProperty(
            "errors",
            exception.bindingResult.fieldErrors.associate { error ->
                error.field to (error.defaultMessage ?: "Invalid value")
            },
        )

        return ResponseEntity.badRequest().body(problem)
    }
}

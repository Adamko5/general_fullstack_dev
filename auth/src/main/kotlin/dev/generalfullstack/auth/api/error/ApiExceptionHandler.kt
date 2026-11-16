package dev.generalfullstack.auth.api.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.slf4j.LoggerFactory

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> = response(
        status = exception.status,
        code = exception.code,
        message = exception.message,
        path = request.requestURI,
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "Invalid value") }

        return response(
            status = HttpStatus.BAD_REQUEST,
            code = "validation_failed",
            message = "One or more request fields are invalid.",
            path = request.requestURI,
            fieldErrors = fieldErrors,
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = exception.constraintViolations
            .associate { it.propertyPath.toString() to it.message }

        return response(
            status = HttpStatus.BAD_REQUEST,
            code = "validation_failed",
            message = "One or more values are invalid.",
            path = request.requestURI,
            fieldErrors = fieldErrors,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> = response(
        status = HttpStatus.BAD_REQUEST,
        code = "invalid_request_body",
        message = "The request body is missing or malformed.",
        path = request.requestURI,
    )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleArgumentTypeMismatch(
        exception: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> = response(
        status = HttpStatus.BAD_REQUEST,
        code = "invalid_parameter",
        message = "A request parameter has an invalid value.",
        path = request.requestURI,
    )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(
        exception: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> = response(
        status = HttpStatus.METHOD_NOT_ALLOWED,
        code = "method_not_allowed",
        message = "The HTTP method is not supported for this endpoint.",
        path = request.requestURI,
    )

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(
        exception: NoResourceFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> = response(
        status = HttpStatus.NOT_FOUND,
        code = "not_found",
        message = "The requested endpoint was not found.",
        path = request.requestURI,
    )

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataConflict(
        exception: DataIntegrityViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> = response(
        status = HttpStatus.CONFLICT,
        code = "data_conflict",
        message = "The request conflicts with existing data.",
        path = request.requestURI,
    )

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("Unhandled error while processing {}", request.requestURI, exception)
        return response(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = "internal_error",
            message = "An unexpected error occurred.",
            path = request.requestURI,
        )
    }

    private fun response(
        status: HttpStatus,
        code: String,
        message: String,
        path: String,
        fieldErrors: Map<String, String> = emptyMap(),
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(
        ApiErrorResponse(
            timestamp = java.time.Instant.now(),
            status = status.value(),
            code = code,
            message = message,
            path = path,
            fieldErrors = fieldErrors,
        ),
    )
}

package com.example.auth.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestControllerAdvice
class GlobalApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.valueOf(ex.statusCode.value())
        return ResponseEntity
            .status(status)
            .body(
                buildError(
                    status = status,
                    message = ex.reason ?: defaultMessage(status),
                    path = request.requestURI,
                    traceId = request.getHeader(TRACE_ID_HEADER),
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    fun handleValidationException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val details =
            when (ex) {
                is MethodArgumentNotValidException -> ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
                is BindException -> ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
                else -> emptyList()
            }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                buildError(
                    status = HttpStatus.BAD_REQUEST,
                    code = "VALIDATION_ERROR",
                    message = "Validation failed",
                    details = details,
                    path = request.requestURI,
                    traceId = request.getHeader(TRACE_ID_HEADER),
                ),
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                buildError(
                    status = HttpStatus.BAD_REQUEST,
                    code = "INVALID_REQUEST_BODY",
                    message = "Request body is invalid",
                    path = request.requestURI,
                    traceId = request.getHeader(TRACE_ID_HEADER),
                ),
            )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                buildError(
                    status = HttpStatus.BAD_REQUEST,
                    code = "INVALID_ARGUMENT",
                    message = ex.message ?: "Invalid argument",
                    path = request.requestURI,
                    traceId = request.getHeader(TRACE_ID_HEADER),
                ),
            )

    @ExceptionHandler(Exception::class)
    fun handleException(request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                buildError(
                    status = HttpStatus.INTERNAL_SERVER_ERROR,
                    code = "INTERNAL_ERROR",
                    message = "Internal server error",
                    path = request.requestURI,
                    traceId = request.getHeader(TRACE_ID_HEADER),
                ),
            )

    private fun buildError(
        status: HttpStatus,
        message: String,
        path: String,
        traceId: String?,
        code: String = codeByStatus(status),
        details: List<String>? = null,
    ): ApiErrorResponse =
        ApiErrorResponse(
            code = code,
            message = message,
            details = details?.takeIf { it.isNotEmpty() },
            path = path,
            timestamp = Instant.now(),
            traceId = traceId,
        )

    private fun codeByStatus(status: HttpStatus): String =
        when (status) {
            HttpStatus.BAD_REQUEST -> "BAD_REQUEST"
            HttpStatus.UNAUTHORIZED -> "UNAUTHORIZED"
            HttpStatus.FORBIDDEN -> "FORBIDDEN"
            HttpStatus.NOT_FOUND -> "NOT_FOUND"
            else -> "HTTP_${status.value()}"
        }

    private fun defaultMessage(status: HttpStatus): String =
        when (status) {
            HttpStatus.BAD_REQUEST -> "Bad request"
            HttpStatus.UNAUTHORIZED -> "Unauthorized"
            HttpStatus.FORBIDDEN -> "Forbidden"
            HttpStatus.NOT_FOUND -> "Not found"
            else -> status.reasonPhrase
        }

    private companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
    }
}

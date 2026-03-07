package com.example.auth.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.web.server.ResponseStatusException

class GlobalApiExceptionHandlerTest {
    private val handler = GlobalApiExceptionHandler()

    @Test
    fun `maps ResponseStatusException to unified body`() {
        val request =
            MockHttpServletRequest().apply {
                requestURI = "/test/forbidden"
                addHeader("X-Trace-Id", "trace-123")
            }

        val response = handler.handleResponseStatusException(ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"), request)
        val body = response.body

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNotNull(body)
        assertEquals("FORBIDDEN", body!!.code)
        assertEquals("Access denied", body.message)
        assertEquals("/test/forbidden", body.path)
        assertEquals("trace-123", body.traceId)
        assertNotNull(body.timestamp)
    }

    @Test
    fun `maps validation errors to VALIDATION_ERROR`() {
        val bindingResult = BeanPropertyBindingResult(Any(), "request")
        bindingResult.addError(FieldError("request", "name", "must not be blank"))
        val request = MockHttpServletRequest().apply { requestURI = "/test/validate" }

        val response = handler.handleValidationException(BindException(bindingResult), request)
        val body = response.body

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(body)
        assertEquals("VALIDATION_ERROR", body!!.code)
        assertEquals("Validation failed", body.message)
        assertEquals(listOf("name: must not be blank"), body.details)
        assertEquals("/test/validate", body.path)
        assertNotNull(body.timestamp)
    }

    @Test
    fun `maps illegal argument to INVALID_ARGUMENT`() {
        val request = MockHttpServletRequest().apply { requestURI = "/test/args" }

        val response = handler.handleIllegalArgumentException(IllegalArgumentException("Invalid value"), request)
        val body = response.body

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(body)
        assertEquals("INVALID_ARGUMENT", body!!.code)
        assertEquals("Invalid value", body.message)
        assertEquals("/test/args", body.path)
        assertNotNull(body.timestamp)
    }

    @Test
    fun `maps unexpected exception to INTERNAL_ERROR`() {
        val request = MockHttpServletRequest().apply { requestURI = "/test/fail" }

        val response = handler.handleException(request)
        val body = response.body

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(body)
        assertEquals("INTERNAL_ERROR", body!!.code)
        assertEquals("Internal server error", body.message)
        assertEquals("/test/fail", body.path)
        assertNotNull(body.timestamp)
    }
}

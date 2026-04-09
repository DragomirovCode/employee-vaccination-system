package com.example.auth.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ApiAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            errorJson(
                code = "UNAUTHORIZED",
                message = "Unauthorized",
                path = request.requestURI,
                traceId = request.getHeader("X-Trace-Id"),
            ),
        )
    }

    private fun errorJson(
        code: String,
        message: String,
        path: String,
        traceId: String?,
    ): String =
        """{"code":"$code","message":"$message","path":"$path","timestamp":"${Instant.now()}","traceId":${traceId?.let {
            "\"${escape(
                it,
            )}\""
        } ?: "null"}}"""

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}

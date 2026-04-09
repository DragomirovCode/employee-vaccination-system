package com.example.auth.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ApiAccessDeniedHandler : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            errorJson(
                code = "FORBIDDEN",
                message = "Forbidden",
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

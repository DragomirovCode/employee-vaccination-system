package com.example.reporting.access

import com.example.auth.AppRole
import com.example.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.HandlerInterceptor
import java.util.UUID

@Component
class ReportingSecurityInterceptor(
    private val authService: AuthService,
    private val scopeResolver: ReportingAccessScopeResolver,
) : HandlerInterceptor {
    /**
     * Проверяет, что пользователь имеет право на просмотр отчетов,
     * вычисляет область доступа и сохраняет ее в атрибутах запроса.
     */
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            return true
        }
        if (handler !is HandlerMethod) {
            return true
        }

        val principal = authService.requireAnyRole(REPORTING_ROLES)
        val requestedDepartmentId =
            request.getParameter("departmentId")?.let {
                runCatching { UUID.fromString(it) }.getOrElse {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid departmentId")
                }
            }
        val scope = scopeResolver.resolve(principal, requestedDepartmentId)
        request.setAttribute(ReportingSecurityContext.REPORTING_SCOPE_ATTRIBUTE, scope)
        return true
    }

    private companion object {
        val REPORTING_ROLES = setOf(AppRole.PERSON, AppRole.HR, AppRole.MEDICAL, AppRole.ADMIN)
    }
}

package com.example.auth.api.notification

import com.example.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class NotificationSecurityInterceptor(
    private val authService: AuthService,
) : HandlerInterceptor {
    /**
     * Проверяет аутентификацию пользователя и сохраняет principal в атрибутах запроса.
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

        val principal = authService.requireAuthenticated(request.getHeader("X-Auth-Token"))
        request.setAttribute(NotificationSecurityContext.PRINCIPAL_ATTRIBUTE, principal)
        return true
    }
}

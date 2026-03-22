package com.example.auth.api.admin

import com.example.auth.AppRole
import com.example.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthAdminSecurityInterceptor(
    private val authService: AuthService,
) : HandlerInterceptor {
    /**
     * Проверяет, что запрос выполняется аутентифицированным пользователем с ролью администратора,
     * и сохраняет principal в атрибутах запроса.
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

        val principal = authService.requireAnyRole(request.getHeader("X-Auth-Token"), setOf(AppRole.ADMIN))
        request.setAttribute(AuthAdminSecurityContext.PRINCIPAL_ATTRIBUTE, principal)
        return true
    }
}

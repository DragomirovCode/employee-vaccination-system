package com.example.employee.api.security

import com.example.auth.AppRole
import com.example.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class EmployeeSecurityInterceptor(
    private val authService: AuthService,
) : HandlerInterceptor {
    /**
     * Проверяет токен и определяет требуемый уровень доступа в зависимости от HTTP-метода.
     *
     * Для операций удаления требуется роль администратора, для создания и обновления
     * достаточно ролей HR или ADMIN, для чтения достаточно аутентификации.
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

        val method = request.method.uppercase()
        val principal =
            when {
                method in ADMIN_ONLY_WRITE_METHODS -> {
                    authService.requireAnyRole(request.getHeader("X-Auth-Token"), setOf(AppRole.ADMIN))
                }

                method in GENERAL_WRITE_METHODS -> {
                    authService.requireAnyRole(request.getHeader("X-Auth-Token"), GENERAL_WRITE_ROLES)
                }

                else -> {
                    authService.requireAuthenticated(request.getHeader("X-Auth-Token"))
                }
            }
        request.setAttribute(EmployeeSecurityContext.PRINCIPAL_ATTRIBUTE, principal)
        return true
    }

    private companion object {
        val ADMIN_ONLY_WRITE_METHODS = setOf("DELETE")
        val GENERAL_WRITE_METHODS = setOf("POST", "PUT")
        val GENERAL_WRITE_ROLES = setOf(AppRole.HR, AppRole.ADMIN)
    }
}

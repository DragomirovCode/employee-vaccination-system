package com.example.vaccine.api.security

import com.example.auth.AppRole
import com.example.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class VaccineSecurityInterceptor(
    private val authService: AuthService,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) {
            return true
        }

        val method = request.method.uppercase()
        val principal =
            if (method in WRITE_METHODS) {
                authService.requireAnyRole(request.getHeader("X-Auth-Token"), WRITE_ROLES)
        } else {
                authService.requireAuthenticated(request.getHeader("X-Auth-Token"))
            }
        request.setAttribute(VaccineSecurityContext.PRINCIPAL_ATTRIBUTE, principal)
        return true
    }

    private companion object {
        val WRITE_METHODS = setOf("POST", "PUT", "DELETE")
        val WRITE_ROLES = setOf(AppRole.HR, AppRole.ADMIN)
    }
}

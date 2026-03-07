package com.example.vaccination.api.security

import com.example.auth.AppRole
import com.example.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class VaccinationWriteSecurityInterceptor(
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
        if (method !in WRITE_METHODS) {
            return true
        }

        val principal = authService.requireAnyRole(request.getHeader("X-Auth-Token"), WRITE_ROLES)
        request.setAttribute(VaccinationSecurityContext.PRINCIPAL_ATTRIBUTE, principal)
        return true
    }

    private companion object {
        val WRITE_ROLES = setOf(AppRole.MEDICAL, AppRole.ADMIN)
        val WRITE_METHODS = setOf("POST", "PUT", "DELETE")
    }
}

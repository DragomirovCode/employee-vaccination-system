package com.example.auth

import com.example.auth.role.UserRoleRepository
import com.example.auth.security.AppUserDetails
import com.example.auth.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val authenticationManager: AuthenticationManager,
) {
    @Transactional(readOnly = true)
    fun requireAuthenticated(): AuthenticatedPrincipal = currentPrincipal()

    @Transactional(readOnly = true)
    fun requireAnyRole(allowedRoles: Set<AppRole>): AuthenticatedPrincipal {
        val principal = currentPrincipal()
        if (principal.roles.intersect(allowedRoles).isEmpty()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions")
        }
        return principal
    }

    @Transactional(readOnly = true)
    fun authenticate(
        email: String,
        password: String,
    ): Authentication =
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email.trim(), password),
            )
        } catch (_: AuthenticationException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }

    @Transactional(readOnly = true)
    fun requirePrincipal(authentication: Authentication): AuthenticatedPrincipal {
        val principal = authentication.principal
        return when (principal) {
            is AppUserDetails -> principal.authenticatedPrincipal
            is AuthenticatedPrincipal -> principal
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
        }
    }

    private fun currentPrincipal(): AuthenticatedPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || authentication is AnonymousAuthenticationToken || !authentication.isAuthenticated) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        }

        val principal = requirePrincipal(authentication)
        val user =
            userRepository.findById(principal.userId).orElseThrow {
                ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
            }

        if (!user.isActive) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive")
        }

        val roles =
            userRoleRepository
                .findRoleCodesByUserId(principal.userId)
                .mapNotNull(AppRole::fromCode)
                .toSet()

        return principal.copy(roles = roles)
    }
}

data class AuthenticatedPrincipal(
    val userId: UUID,
    val roles: Set<AppRole>,
)

enum class AppRole {
    PERSON,
    HR,
    MEDICAL,
    ADMIN,
    ;

    companion object {
        fun fromCode(code: String): AppRole? = entries.firstOrNull { it.name == code }
    }
}

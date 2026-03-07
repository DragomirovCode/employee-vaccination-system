package com.example.auth

import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
) {
    @Transactional(readOnly = true)
    fun requireAuthenticated(token: String?): AuthenticatedPrincipal = authenticate(token)

    @Transactional(readOnly = true)
    fun requireAnyRole(
        token: String?,
        allowedRoles: Set<AppRole>,
    ): AuthenticatedPrincipal {
        val principal = authenticate(token)
        if (principal.roles.intersect(allowedRoles).isEmpty()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions")
        }
        return principal
    }

    private fun authenticate(token: String?): AuthenticatedPrincipal {
        val userId =
            parseUserId(token)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token")

        val user =
            userRepository.findById(userId).orElseThrow {
                ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
            }

        if (!user.isActive) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive")
        }

        val roles =
            userRoleRepository
                .findRoleCodesByUserId(userId)
                .mapNotNull(AppRole::fromCode)
                .toSet()

        return AuthenticatedPrincipal(
            userId = userId,
            roles = roles,
        )
    }

    private fun parseUserId(token: String?): UUID? {
        val rawToken = token?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val normalized = rawToken.removePrefix("Bearer ").trim()
        return runCatching { UUID.fromString(normalized) }.getOrNull()
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

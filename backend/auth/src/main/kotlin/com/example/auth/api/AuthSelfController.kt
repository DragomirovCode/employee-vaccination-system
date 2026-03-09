package com.example.auth.api

import com.example.auth.AppRole
import com.example.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authenticated user endpoints")
class AuthSelfController(
    private val authService: AuthService,
) {
    @GetMapping("/me")
    @Operation(summary = "Get authenticated user info")
    fun me(
        @RequestHeader("X-Auth-Token", required = false) token: String?,
    ): AuthMeResponse {
        val principal = authService.requireAuthenticated(token)
        return AuthMeResponse(
            userId = principal.userId,
            roles = principal.roles.toList().sortedBy { it.name },
        )
    }
}

data class AuthMeResponse(
    val userId: UUID,
    val roles: List<AppRole>,
)

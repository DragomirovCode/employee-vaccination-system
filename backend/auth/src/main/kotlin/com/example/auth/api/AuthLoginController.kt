package com.example.auth.api

import com.example.auth.AppRole
import com.example.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
class AuthLoginController(
    private val authService: AuthService,
    private val securityContextRepository: SecurityContextRepository,
) {
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authenticate by email and password and create a session cookie")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Authenticated. Response sets JSESSIONID cookie."),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun login(
        @RequestBody body: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): AuthSessionResponse {
        val authentication = authService.authenticate(body.email, body.password)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)

        val principal = authService.requirePrincipal(authentication)
        return AuthSessionResponse(
            userId = principal.userId,
            roles = principal.roles.toList().sortedBy { it.name },
        )
    }
}

data class LoginRequest(
    val email: String,
    val password: String,
)

data class AuthSessionResponse(
    val userId: UUID,
    val roles: List<AppRole>,
)

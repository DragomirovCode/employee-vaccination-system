package com.example.auth.api

import com.example.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Session-based authentication endpoints")
class AuthSelfController(
    private val authService: AuthService,
) {
    @GetMapping("/me")
    @Operation(summary = "Get authenticated user from the active HTTP session")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Authenticated user resolved from session"),
            ApiResponse(
                responseCode = "401",
                description = "No active authenticated session",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun me(): AuthSessionResponse {
        val principal = authService.requireAuthenticated()
        return AuthSessionResponse(
            userId = principal.userId,
            roles = principal.roles.toList().sortedBy { it.name },
        )
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Invalidate the current HTTP session")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Session invalidated, JSESSIONID removed"),
        ],
    )
    fun logout() {
        // Spring Security logout filter handles this endpoint before the controller method executes.
    }
}

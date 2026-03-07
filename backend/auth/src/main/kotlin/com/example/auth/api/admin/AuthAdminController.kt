package com.example.auth.api.admin

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.auth.role.RoleEntity
import com.example.auth.role.UserRoleEntity
import com.example.auth.user.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth Admin", description = "Administrative auth endpoints")
class AuthAdminController(
    private val authAdminService: AuthAdminService,
) {
    @GetMapping("/users")
    @Operation(summary = "Get users list")
    fun listUsers(): List<AuthUserResponse> = authAdminService.listUsers().map(AuthUserResponse::fromEntity)

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by id")
    fun getUser(
        @PathVariable id: UUID,
    ): AuthUserResponse = AuthUserResponse.fromEntity(authAdminService.getUser(id))

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "User created"),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "Email conflict", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun createUser(
        @RequestBody body: AuthUserWriteRequest,
    ): AuthUserResponse =
        AuthUserResponse.fromEntity(
            authAdminService.createUser(
                CreateUserCommand(
                    email = body.email,
                    passwordHash = body.passwordHash,
                    isActive = body.isActive,
                ),
            ),
        )

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User updated"),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "Email conflict", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun updateUser(
        @PathVariable id: UUID,
        @RequestBody body: AuthUserWriteRequest,
    ): AuthUserResponse =
        AuthUserResponse.fromEntity(
            authAdminService.updateUser(
                id = id,
                command =
                    UpdateUserCommand(
                        email = body.email,
                        passwordHash = body.passwordHash,
                        isActive = body.isActive,
                    ),
            ),
        )

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Set user active status")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Status updated"),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun setStatus(
        @PathVariable id: UUID,
        @RequestBody body: AuthUserStatusRequest,
    ): AuthUserResponse = AuthUserResponse.fromEntity(authAdminService.setStatus(id, body.active))

    @GetMapping("/roles")
    @Operation(summary = "Get roles list")
    fun listRoles(): List<AuthRoleResponse> = authAdminService.listRoles().map(AuthRoleResponse::fromEntity)

    @GetMapping("/users/{id}/roles")
    @Operation(summary = "Get roles assigned to user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User roles"),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun listUserRoles(
        @PathVariable id: UUID,
    ): List<AuthUserRoleResponse> = authAdminService.listUserRoles(id).map(AuthUserRoleResponse::fromEntity)

    @PostMapping("/users/{id}/roles/{roleCode}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign role to user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Role assigned"),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User or role not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "Role already assigned", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun assignRole(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @PathVariable roleCode: String,
    ): AuthUserRoleResponse {
        val principal = requirePrincipal(request)
        return AuthUserRoleResponse.fromEntity(authAdminService.assignRole(id, roleCode, principal.userId))
    }

    @DeleteMapping("/users/{id}/roles/{roleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove role from user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Role removed"),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Assignment not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun unassignRole(
        @PathVariable id: UUID,
        @PathVariable roleCode: String,
    ) {
        authAdminService.unassignRole(id, roleCode)
    }

    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(AuthAdminSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

data class AuthUserWriteRequest(
    val email: String,
    val passwordHash: String,
    val isActive: Boolean = true,
)

data class AuthUserStatusRequest(
    val active: Boolean,
)

data class AuthUserResponse(
    val id: UUID,
    val email: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: UserEntity): AuthUserResponse =
            AuthUserResponse(
                id = entity.id!!,
                email = entity.email,
                isActive = entity.isActive,
                createdAt = entity.createdAt!!,
                updatedAt = entity.updatedAt!!,
            )
    }
}

data class AuthRoleResponse(
    val id: Int,
    val code: String,
    val name: String,
) {
    companion object {
        fun fromEntity(entity: RoleEntity): AuthRoleResponse =
            AuthRoleResponse(
                id = entity.id!!,
                code = entity.code,
                name = entity.name,
            )
    }
}

data class AuthUserRoleResponse(
    val userId: UUID,
    val roleId: Int,
    val assignedAt: Instant,
    val assignedBy: UUID?,
) {
    companion object {
        fun fromEntity(entity: UserRoleEntity): AuthUserRoleResponse =
            AuthUserRoleResponse(
                userId = entity.id.userId!!,
                roleId = entity.id.roleId!!,
                assignedAt = entity.assignedAt!!,
                assignedBy = entity.assignedBy,
            )
    }
}

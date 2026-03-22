package com.example.employee.api

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.employee.api.security.EmployeeSecurityContext
import com.example.employee.person.CreateEmployeeCommand
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeService
import com.example.employee.person.UpdateEmployeeCommand
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/employees")
@Tag(name = "Employees", description = "Employee management endpoints")
class EmployeeController(
    private val employeeService: EmployeeService,
) {
    /** Возвращает список сотрудников, доступных текущему пользователю. */
    @GetMapping
    @Operation(summary = "Get employees list")
    fun list(request: HttpServletRequest): List<EmployeeResponse> =
        employeeService.list(requirePrincipal(request)).map(EmployeeResponse::fromEntity)

    /** Возвращает сотрудника по идентификатору. */
    @GetMapping("/{id}")
    @Operation(summary = "Get employee by id")
    fun get(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ): EmployeeResponse = EmployeeResponse.fromEntity(employeeService.get(id, requirePrincipal(request)))

    /** Создает нового сотрудника. */
    @PostMapping
    @Operation(summary = "Create employee")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Employee created"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict: userId already assigned",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        request: HttpServletRequest,
        @RequestBody body: EmployeeWriteRequest,
    ): EmployeeResponse =
        EmployeeResponse.fromEntity(
            employeeService.create(
                CreateEmployeeCommand(
                    userId = body.userId,
                    departmentId = body.departmentId,
                    firstName = body.firstName,
                    lastName = body.lastName,
                    middleName = body.middleName,
                    birthDate = body.birthDate,
                    position = body.position,
                    hireDate = body.hireDate,
                ),
                performedBy = requirePrincipal(request).userId,
            ),
        )

    /** Обновляет данные сотрудника. */
    @PutMapping("/{id}")
    @Operation(summary = "Update employee")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Employee updated"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Employee not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict: userId already assigned",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun update(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @RequestBody body: EmployeeWriteRequest,
    ): EmployeeResponse =
        EmployeeResponse.fromEntity(
            employeeService.update(
                id = id,
                command =
                    UpdateEmployeeCommand(
                        userId = body.userId,
                        departmentId = body.departmentId,
                        firstName = body.firstName,
                        lastName = body.lastName,
                        middleName = body.middleName,
                        birthDate = body.birthDate,
                        position = body.position,
                        hireDate = body.hireDate,
                    ),
                performedBy = requirePrincipal(request).userId,
            ),
        )

    /** Удаляет сотрудника. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete employee")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Employee deleted"),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Employee not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun delete(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ) {
        employeeService.delete(id, requirePrincipal(request).userId)
    }

    /**
     * Извлекает аутентифицированного пользователя из атрибутов запроса.
     */
    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(EmployeeSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

data class EmployeeWriteRequest(
    val userId: UUID? = null,
    val departmentId: UUID,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val birthDate: LocalDate? = null,
    val position: String? = null,
    val hireDate: LocalDate? = null,
)

data class EmployeeResponse(
    val id: UUID,
    val userId: UUID?,
    val departmentId: UUID,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val birthDate: LocalDate?,
    val position: String?,
    val hireDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        /** Преобразует сущность сотрудника в DTO ответа API. */
        fun fromEntity(entity: EmployeeEntity): EmployeeResponse =
            EmployeeResponse(
                id = entity.id!!,
                userId = entity.userId,
                departmentId = entity.departmentId!!,
                firstName = entity.firstName,
                lastName = entity.lastName,
                middleName = entity.middleName,
                birthDate = entity.birthDate,
                position = entity.position,
                hireDate = entity.hireDate,
                createdAt = entity.createdAt!!,
                updatedAt = entity.updatedAt!!,
            )
    }
}

package com.example.employee.api

import com.example.auth.api.ApiErrorResponse
import com.example.employee.department.CreateDepartmentCommand
import com.example.employee.department.DepartmentService
import com.example.employee.department.UpdateDepartmentCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/departments")
@Tag(name = "Departments", description = "Department management endpoints")
class DepartmentController(
    private val departmentService: DepartmentService,
) {
    @GetMapping
    @Operation(summary = "Get departments list")
    fun list(): List<DepartmentResponse> = departmentService.list().map(DepartmentResponse::fromEntity)

    @GetMapping("/{id}")
    @Operation(summary = "Get department by id")
    fun get(
        @PathVariable id: UUID,
    ): DepartmentResponse = DepartmentResponse.fromEntity(departmentService.get(id))

    @PostMapping
    @Operation(summary = "Create department")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Department created"),
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
        ],
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody body: DepartmentWriteRequest,
    ): DepartmentResponse =
        DepartmentResponse.fromEntity(
            departmentService.create(
                CreateDepartmentCommand(
                    name = body.name,
                    parentId = body.parentId,
                ),
            ),
        )

    @PutMapping("/{id}")
    @Operation(summary = "Update department")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Department updated"),
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
                description = "Department not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: DepartmentWriteRequest,
    ): DepartmentResponse =
        DepartmentResponse.fromEntity(
            departmentService.update(
                id = id,
                command =
                    UpdateDepartmentCommand(
                        name = body.name,
                        parentId = body.parentId,
                    ),
            ),
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete department")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Department deleted"),
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
                description = "Department not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Department has child departments or employees",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun delete(
        @PathVariable id: UUID,
    ) {
        departmentService.delete(id)
    }
}

data class DepartmentWriteRequest(
    val name: String,
    val parentId: UUID? = null,
)

data class DepartmentResponse(
    val id: UUID,
    val name: String,
    val parentId: UUID?,
    val createdAt: Instant,
) {
    companion object {
        fun fromEntity(entity: com.example.employee.department.DepartmentEntity): DepartmentResponse =
            DepartmentResponse(
                id = entity.id!!,
                name = entity.name,
                parentId = entity.parentId,
                createdAt = entity.createdAt!!,
            )
    }
}

package com.example.vaccine.api

import com.example.auth.api.ApiErrorResponse
import com.example.vaccine.disease.CreateDiseaseCommand
import com.example.vaccine.disease.DiseaseEntity
import com.example.vaccine.disease.DiseaseService
import com.example.vaccine.disease.UpdateDiseaseCommand
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

@RestController
@RequestMapping("/diseases")
@Tag(name = "Diseases", description = "Disease dictionary management")
class DiseaseController(
    private val diseaseService: DiseaseService,
) {
    @GetMapping
    @Operation(summary = "Get diseases list")
    fun list(): List<DiseaseResponse> = diseaseService.list().map(DiseaseResponse::fromEntity)

    @GetMapping("/{id}")
    @Operation(summary = "Get disease by id")
    fun get(
        @PathVariable id: Int,
    ): DiseaseResponse = DiseaseResponse.fromEntity(diseaseService.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create disease")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Disease created"),
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
                description = "Conflict",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun create(
        @RequestBody body: DiseaseWriteRequest,
    ): DiseaseResponse =
        DiseaseResponse.fromEntity(
            diseaseService.create(
                CreateDiseaseCommand(
                    name = body.name,
                    description = body.description,
                ),
            ),
        )

    @PutMapping("/{id}")
    @Operation(summary = "Update disease")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Disease updated"),
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
                description = "Not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun update(
        @PathVariable id: Int,
        @RequestBody body: DiseaseWriteRequest,
    ): DiseaseResponse =
        DiseaseResponse.fromEntity(
            diseaseService.update(
                id = id,
                command =
                    UpdateDiseaseCommand(
                        name = body.name,
                        description = body.description,
                    ),
            ),
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete disease")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Disease deleted"),
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
                description = "Not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun delete(
        @PathVariable id: Int,
    ) {
        diseaseService.delete(id)
    }
}

data class DiseaseWriteRequest(
    val name: String,
    val description: String? = null,
)

data class DiseaseResponse(
    val id: Int,
    val name: String,
    val description: String?,
) {
    companion object {
        fun fromEntity(entity: DiseaseEntity): DiseaseResponse =
            DiseaseResponse(
                id = entity.id!!,
                name = entity.name,
                description = entity.description,
            )
    }
}

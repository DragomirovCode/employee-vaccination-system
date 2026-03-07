package com.example.vaccine.api

import com.example.auth.api.ApiErrorResponse
import com.example.vaccine.vaccine.CreateVaccineCommand
import com.example.vaccine.vaccine.UpdateVaccineCommand
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineService
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
@RequestMapping("/vaccines")
@Tag(name = "Vaccines", description = "Vaccine dictionary management")
class VaccineController(
    private val vaccineService: VaccineService,
) {
    @GetMapping
    @Operation(summary = "Get vaccines list")
    fun list(): List<VaccineResponse> = vaccineService.list().map(VaccineResponse::fromEntity)

    @GetMapping("/{id}")
    @Operation(summary = "Get vaccine by id")
    fun get(
        @PathVariable id: UUID,
    ): VaccineResponse = VaccineResponse.fromEntity(vaccineService.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create vaccine")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Vaccine created"),
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
        @RequestBody body: VaccineWriteRequest,
    ): VaccineResponse =
        VaccineResponse.fromEntity(
            vaccineService.create(
                CreateVaccineCommand(
                    name = body.name,
                    manufacturer = body.manufacturer,
                    validityDays = body.validityDays,
                    dosesRequired = body.dosesRequired,
                    daysBetween = body.daysBetween,
                    isActive = body.isActive,
                ),
            ),
        )

    @PutMapping("/{id}")
    @Operation(summary = "Update vaccine")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vaccine updated"),
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
        @PathVariable id: UUID,
        @RequestBody body: VaccineWriteRequest,
    ): VaccineResponse =
        VaccineResponse.fromEntity(
            vaccineService.update(
                id = id,
                command =
                    UpdateVaccineCommand(
                        name = body.name,
                        manufacturer = body.manufacturer,
                        validityDays = body.validityDays,
                        dosesRequired = body.dosesRequired,
                        daysBetween = body.daysBetween,
                        isActive = body.isActive,
                    ),
            ),
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete vaccine")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Vaccine deleted"),
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
        @PathVariable id: UUID,
    ) {
        vaccineService.delete(id)
    }
}

data class VaccineWriteRequest(
    val name: String,
    val manufacturer: String? = null,
    val validityDays: Int,
    val dosesRequired: Int,
    val daysBetween: Int? = null,
    val isActive: Boolean = true,
)

data class VaccineResponse(
    val id: UUID,
    val name: String,
    val manufacturer: String?,
    val validityDays: Int,
    val dosesRequired: Int,
    val daysBetween: Int?,
    val isActive: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun fromEntity(entity: VaccineEntity): VaccineResponse =
            VaccineResponse(
                id = entity.id!!,
                name = entity.name,
                manufacturer = entity.manufacturer,
                validityDays = entity.validityDays,
                dosesRequired = entity.dosesRequired,
                daysBetween = entity.daysBetween,
                isActive = entity.isActive,
                createdAt = entity.createdAt!!,
            )
    }
}

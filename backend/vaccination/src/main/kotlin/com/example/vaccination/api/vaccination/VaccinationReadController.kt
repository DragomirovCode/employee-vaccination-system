package com.example.vaccination.api.vaccination

import com.example.auth.AppRole
import com.example.auth.AuthService
import com.example.auth.api.ApiErrorResponse
import com.example.vaccination.api.read.VaccinationReadFilter
import com.example.vaccination.api.read.VaccinationReadService
import com.example.vaccination.vaccination.VaccinationEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RestController
@Tag(name = "Vaccinations", description = "Read operations for vaccinations")
class VaccinationReadController(
    private val readService: VaccinationReadService,
    private val authService: AuthService,
) {
    @GetMapping("/vaccinations/{id}")
    @Operation(summary = "Get vaccination by id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vaccination card"),
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
                description = "Vaccination not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun getById(
        @PathVariable id: UUID,
    ): VaccinationReadResponse =
        VaccinationReadResponse.fromEntity(
            readService.getVaccination(
                authService.requireAnyRole(setOf(AppRole.PERSON, AppRole.HR, AppRole.MEDICAL, AppRole.ADMIN)),
                id,
            ),
        )

    @GetMapping("/vaccinations")
    @Operation(summary = "Get vaccinations list with filters and pagination")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vaccinations page"),
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
    fun list(
        @Parameter(description = "Optional employee filter")
        @RequestParam(required = false)
        employeeId: UUID?,
        @Parameter(description = "Optional vaccine filter")
        @RequestParam(required = false)
        vaccineId: UUID?,
        @Parameter(description = "Optional date from (inclusive)")
        @RequestParam(required = false)
        dateFrom: LocalDate?,
        @Parameter(description = "Optional date to (inclusive)")
        @RequestParam(required = false)
        dateTo: LocalDate?,
        @RequestParam(defaultValue = "0")
        page: Int,
        @RequestParam(defaultValue = "20")
        size: Int,
    ): Page<VaccinationReadResponse> =
        readService
            .listVaccinations(
                principal = authService.requireAnyRole(setOf(AppRole.PERSON, AppRole.HR, AppRole.MEDICAL, AppRole.ADMIN)),
                filter =
                    VaccinationReadFilter(
                        employeeId = employeeId,
                        vaccineId = vaccineId,
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                    ),
                pageable = PageRequest.of(page, size),
            ).map(VaccinationReadResponse::fromEntity)

    @GetMapping("/employees/{employeeId}/vaccinations")
    @Operation(summary = "Get vaccination history by employee")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vaccination history"),
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
    fun listByEmployee(
        @PathVariable employeeId: UUID,
    ): List<VaccinationReadResponse> =
        readService
            .listEmployeeVaccinations(
                principal = authService.requireAnyRole(setOf(AppRole.PERSON, AppRole.HR, AppRole.MEDICAL, AppRole.ADMIN)),
                employeeId = employeeId,
            ).map(VaccinationReadResponse::fromEntity)
}

data class VaccinationReadResponse(
    val id: UUID,
    val employeeId: UUID,
    val vaccineId: UUID,
    val performedBy: UUID,
    val vaccinationDate: LocalDate,
    val doseNumber: Int,
    val batchNumber: String?,
    val expirationDate: LocalDate?,
    val nextDoseDate: LocalDate?,
    val revaccinationDate: LocalDate?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: VaccinationEntity): VaccinationReadResponse =
            VaccinationReadResponse(
                id = entity.id!!,
                employeeId = entity.employeeId!!,
                vaccineId = entity.vaccineId!!,
                performedBy = entity.performedBy!!,
                vaccinationDate = entity.vaccinationDate!!,
                doseNumber = entity.doseNumber,
                batchNumber = entity.batchNumber,
                expirationDate = entity.expirationDate,
                nextDoseDate = entity.nextDoseDate,
                revaccinationDate = entity.revaccinationDate,
                notes = entity.notes,
                createdAt = entity.createdAt!!,
                updatedAt = entity.updatedAt!!,
            )
    }
}

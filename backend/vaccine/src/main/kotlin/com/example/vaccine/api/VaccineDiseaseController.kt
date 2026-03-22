package com.example.vaccine.api

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.vaccine.api.security.VaccineSecurityContext
import com.example.vaccine.vaccinedisease.VaccineDiseaseEntity
import com.example.vaccine.vaccinedisease.VaccineDiseaseService
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/vaccines/{vaccineId}/diseases")
@Tag(name = "Vaccine-Disease Links", description = "Manage vaccine to disease associations")
class VaccineDiseaseController(
    private val vaccineDiseaseService: VaccineDiseaseService,
) {
    /** Возвращает список связей заболевания с вакциной. */
    @GetMapping
    @Operation(summary = "Get disease links for vaccine")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Links list"),
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
        ],
    )
    fun listByVaccine(
        @PathVariable vaccineId: UUID,
    ): List<VaccineDiseaseLinkResponse> = vaccineDiseaseService.listByVaccine(vaccineId).map(VaccineDiseaseLinkResponse::fromEntity)

    /** Создает связь вакцины и заболевания. */
    @PostMapping("/{diseaseId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create vaccine-disease link")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Link created"),
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
    fun createLink(
        request: HttpServletRequest,
        @PathVariable vaccineId: UUID,
        @PathVariable diseaseId: Int,
    ) {
        vaccineDiseaseService.createLink(vaccineId, diseaseId, requirePrincipal(request).userId)
    }

    /** Удаляет связь вакцины и заболевания. */
    @DeleteMapping("/{diseaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete vaccine-disease link")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Link deleted"),
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
    fun deleteLink(
        request: HttpServletRequest,
        @PathVariable vaccineId: UUID,
        @PathVariable diseaseId: Int,
    ) {
        vaccineDiseaseService.deleteLink(vaccineId, diseaseId, requirePrincipal(request).userId)
    }

    /**
     * Извлекает аутентифицированного пользователя из атрибутов запроса.
     */
    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(VaccineSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

/**
 * DTO ответа со связью вакцины и заболевания.
 */
data class VaccineDiseaseLinkResponse(
    /** Идентификатор вакцины. */
    val vaccineId: UUID,
    /** Идентификатор заболевания. */
    val diseaseId: Int,
) {
    companion object {
        /** Преобразует сущность связи в DTO ответа API. */
        fun fromEntity(entity: VaccineDiseaseEntity): VaccineDiseaseLinkResponse =
            VaccineDiseaseLinkResponse(
                vaccineId = entity.id.vaccineId!!,
                diseaseId = entity.id.diseaseId!!,
            )
    }
}

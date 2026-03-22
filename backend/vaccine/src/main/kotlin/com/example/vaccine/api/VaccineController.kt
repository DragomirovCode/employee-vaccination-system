package com.example.vaccine.api

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.vaccine.api.security.VaccineSecurityContext
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
import java.util.UUID

@RestController
@RequestMapping("/vaccines")
@Tag(name = "Vaccines", description = "Vaccine dictionary management")
class VaccineController(
    private val vaccineService: VaccineService,
) {
    @GetMapping
    @Operation(summary = "Get vaccines list")
    /** Возвращает список вакцин. */
    fun list(): List<VaccineResponse> = vaccineService.list().map(VaccineResponse::fromEntity)

    @GetMapping("/{id}")
    @Operation(summary = "Get vaccine by id")
    /** Возвращает вакцину по идентификатору. */
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
    /** Создает вакцину. */
    fun create(
        request: HttpServletRequest,
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
                performedBy = requirePrincipal(request).userId,
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
    /** Обновляет вакцину. */
    fun update(
        request: HttpServletRequest,
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
                performedBy = requirePrincipal(request).userId,
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
    /** Удаляет вакцину. */
    fun delete(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ) {
        vaccineService.delete(id, requirePrincipal(request).userId)
    }

    /**
     * Извлекает аутентифицированного пользователя из атрибутов запроса.
     */
    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(VaccineSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

/**
 * Тело запроса на создание или обновление вакцины.
 */
data class VaccineWriteRequest(
    /** Наименование вакцины. */
    val name: String,
    /** Производитель вакцины. */
    val manufacturer: String? = null,
    /** Срок действия вакцинации в днях. */
    val validityDays: Int,
    /** Требуемое количество доз. */
    val dosesRequired: Int,
    /** Интервал между дозами в днях. */
    val daysBetween: Int? = null,
    /** Признак активности вакцины. */
    val isActive: Boolean = true,
)

/**
 * DTO ответа с данными о вакцине.
 */
data class VaccineResponse(
    /** Идентификатор вакцины. */
    val id: UUID,
    /** Наименование вакцины. */
    val name: String,
    /** Производитель вакцины. */
    val manufacturer: String?,
    /** Срок действия вакцинации в днях. */
    val validityDays: Int,
    /** Требуемое количество доз. */
    val dosesRequired: Int,
    /** Интервал между дозами в днях. */
    val daysBetween: Int?,
    /** Признак активности вакцины. */
    val isActive: Boolean,
    /** Момент создания записи. */
    val createdAt: Instant,
) {
    companion object {
        /** Преобразует сущность вакцины в DTO ответа API. */
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

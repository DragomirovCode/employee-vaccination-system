package com.example.vaccination.api.vaccination

import com.example.auth.AuthenticatedPrincipal
import com.example.vaccination.api.security.VaccinationSecurityContext
import com.example.vaccination.api.security.VaccinationWriteScopeService
import com.example.vaccination.vaccination.CreateVaccinationCommand
import com.example.vaccination.vaccination.UpdateVaccinationCommand
import com.example.vaccination.vaccination.VaccinationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/vaccinations")
@Tag(name = "Vaccinations", description = "Write operations for vaccinations")
class VaccinationWriteController(
    private val vaccinationService: VaccinationService,
    private val scopeService: VaccinationWriteScopeService,
) {
    @PostMapping
    @Operation(summary = "Create vaccination")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vaccination created"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun create(
        request: HttpServletRequest,
        @RequestBody body: VaccinationWriteRequest,
    ): VaccinationWriteResponse {
        val principal = requirePrincipal(request)
        scopeService.assertVaccinationCreateAllowed(principal, body.employeeId)
        val created =
            vaccinationService.create(
                CreateVaccinationCommand(
                    employeeId = body.employeeId,
                    vaccineId = body.vaccineId,
                    performedBy = principal.userId,
                    vaccinationDate = body.vaccinationDate,
                    doseNumber = body.doseNumber,
                    batchNumber = body.batchNumber,
                    expirationDate = body.expirationDate,
                    notes = body.notes,
                ),
            )

        return VaccinationWriteResponse(id = created.id!!)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vaccination")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vaccination updated"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
        ],
    )
    fun update(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @RequestBody body: VaccinationWriteRequest,
    ): VaccinationWriteResponse {
        val principal = requirePrincipal(request)
        scopeService.assertVaccinationUpdateAllowed(principal, id, body.employeeId)
        val updated =
            vaccinationService.update(
                id = id,
                command =
                    UpdateVaccinationCommand(
                        employeeId = body.employeeId,
                        vaccineId = body.vaccineId,
                        performedBy = principal.userId,
                        vaccinationDate = body.vaccinationDate,
                        doseNumber = body.doseNumber,
                        batchNumber = body.batchNumber,
                        expirationDate = body.expirationDate,
                        notes = body.notes,
                    ),
            )

        return VaccinationWriteResponse(id = updated.id!!)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete vaccination")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Vaccination deleted"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
        ],
    )
    fun delete(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ) {
        val principal = requirePrincipal(request)
        scopeService.assertVaccinationDeleteAllowed(principal, id)
        vaccinationService.delete(id = id, deletedBy = principal.userId)
    }

    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(VaccinationSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

data class VaccinationWriteRequest(
    val employeeId: UUID,
    val vaccineId: UUID,
    val vaccinationDate: LocalDate,
    val doseNumber: Int,
    val batchNumber: String? = null,
    val expirationDate: LocalDate? = null,
    val notes: String? = null,
)

data class VaccinationWriteResponse(
    val id: UUID,
)

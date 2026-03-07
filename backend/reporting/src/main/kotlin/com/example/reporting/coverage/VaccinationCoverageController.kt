package com.example.reporting.coverage

import com.example.reporting.access.ReportingAccessScope
import com.example.reporting.access.ReportingSecurityContext
import jakarta.servlet.http.HttpServletRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/reports")
@Tag(name = "Reporting", description = "Read-only reporting endpoints")
class VaccinationCoverageController(
    private val service: VaccinationCoverageService,
) {
    @GetMapping("/vaccination-coverage")
    @Operation(
        summary = "Get vaccination coverage by departments",
        description = "Returns vaccination coverage metrics grouped by department for a selected period",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Coverage report",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = VaccinationCoverageItem::class)),
                    ),
                ],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
        ],
    )
    fun getVaccinationCoverage(
        request: HttpServletRequest,
        @Parameter(description = "Period start date (inclusive)", example = "2026-01-01")
        @RequestParam
        dateFrom: LocalDate,
        @Parameter(description = "Period end date (inclusive)", example = "2026-12-31")
        @RequestParam
        dateTo: LocalDate,
        @Parameter(description = "Optional department filter", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestParam(required = false)
        departmentId: UUID?,
    ): List<VaccinationCoverageItem> {
        if (dateFrom.isAfter(dateTo)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFrom must be <= dateTo")
        }

        val scope = requireScope(request)

        return service.getCoverageByDepartment(
            dateFrom = dateFrom,
            dateTo = dateTo,
            scope = scope,
        )
    }

    private fun requireScope(request: HttpServletRequest): ReportingAccessScope =
        request.getAttribute(ReportingSecurityContext.REPORTING_SCOPE_ATTRIBUTE) as? ReportingAccessScope
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security scope")
}

package com.example.reporting.coverage

import com.example.auth.api.ApiErrorResponse
import com.example.reporting.access.ReportingAccessScope
import com.example.reporting.access.ReportingSecurityContext
import com.example.reporting.export.ReportExportViewModels
import com.example.reporting.export.ReportExportService
import com.example.reporting.export.ReportFormat
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID
import java.util.Locale

@RestController
@RequestMapping("/reports")
@Tag(name = "Reporting", description = "Read-only reporting endpoints")
class VaccinationCoverageController(
    private val service: VaccinationCoverageService,
    private val reportExportService: ReportExportService,
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
            ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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

    @GetMapping("/vaccination-coverage/export")
    @Operation(summary = "Export vaccination coverage report (csv, xlsx, pdf)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Report export (csv/xlsx/pdf)"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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
    fun exportVaccinationCoverage(
        request: HttpServletRequest,
        @RequestParam dateFrom: LocalDate,
        @RequestParam dateTo: LocalDate,
        @RequestParam(required = false) departmentId: UUID?,
        @RequestParam(defaultValue = "csv") format: String,
    ): ResponseEntity<ByteArray> {
        if (dateFrom.isAfter(dateTo)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFrom must be <= dateTo")
        }
        val reportFormat =
            ReportFormat.fromRaw(format)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported export format")
        val scope = requireScope(request)
        val rows = service.getCoverageByDepartment(dateFrom = dateFrom, dateTo = dateTo, scope = scope)
        val exportView = ReportExportViewModels.coverageByDepartment(rows, resolveExportLocale(request))
        val reportFile =
            reportExportService.export(
                format = reportFormat,
                fileNameBase = "vaccination-coverage",
                headers = exportView.headers,
                rows = exportView.rows,
            )

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${reportFile.fileName}\"")
            .contentType(MediaType.parseMediaType(reportFile.contentType))
            .body(reportFile.bytes)
    }

    @GetMapping("/vaccination-coverage-by-vaccine")
    @Operation(
        summary = "Get vaccination coverage by vaccines",
        description = "Returns vaccination coverage metrics grouped by vaccine for a selected period",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Coverage report",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = VaccinationCoverageByVaccineItem::class)),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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
    fun getVaccinationCoverageByVaccine(
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
    ): List<VaccinationCoverageByVaccineItem> {
        if (dateFrom.isAfter(dateTo)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFrom must be <= dateTo")
        }

        val scope = requireScope(request)

        return service.getCoverageByVaccine(
            dateFrom = dateFrom,
            dateTo = dateTo,
            scope = scope,
        )
    }

    @GetMapping("/vaccination-coverage-by-vaccine/export")
    @Operation(summary = "Export vaccination coverage by vaccines report (csv, xlsx, pdf)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Report export (csv/xlsx/pdf)"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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
    fun exportVaccinationCoverageByVaccine(
        request: HttpServletRequest,
        @RequestParam dateFrom: LocalDate,
        @RequestParam dateTo: LocalDate,
        @RequestParam(required = false) departmentId: UUID?,
        @RequestParam(defaultValue = "csv") format: String,
    ): ResponseEntity<ByteArray> {
        if (dateFrom.isAfter(dateTo)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFrom must be <= dateTo")
        }
        val reportFormat =
            ReportFormat.fromRaw(format)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported export format")
        val scope = requireScope(request)
        val rows = service.getCoverageByVaccine(dateFrom = dateFrom, dateTo = dateTo, scope = scope)
        val exportView = ReportExportViewModels.coverageByVaccine(rows, resolveExportLocale(request))
        val reportFile =
            reportExportService.export(
                format = reportFormat,
                fileNameBase = "vaccination-coverage-by-vaccine",
                headers = exportView.headers,
                rows = exportView.rows,
            )

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${reportFile.fileName}\"")
            .contentType(MediaType.parseMediaType(reportFile.contentType))
            .body(reportFile.bytes)
    }

    private fun requireScope(request: HttpServletRequest): ReportingAccessScope =
        request.getAttribute(ReportingSecurityContext.REPORTING_SCOPE_ATTRIBUTE) as? ReportingAccessScope
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security scope")

    private fun resolveExportLocale(request: HttpServletRequest): Locale =
        request.getHeader("Accept-Language")
            ?.takeIf { it.isNotBlank() }
            ?.let(Locale::forLanguageTag)
            ?: Locale.ENGLISH
}

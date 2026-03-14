package com.example.reporting.revaccination

import com.example.auth.api.ApiErrorResponse
import com.example.reporting.access.ReportingAccessScope
import com.example.reporting.export.ReportExportViewModels
import com.example.reporting.access.ReportingSecurityContext
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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.util.Locale

@RestController
@RequestMapping("/reports")
@Tag(name = "Reporting", description = "Read-only reporting endpoints")
class RevaccinationDueController(
    private val service: RevaccinationDueService,
    private val reportExportService: ReportExportService,
) {
    @GetMapping("/revaccination-due")
    @Operation(
        summary = "Get employees due for revaccination",
        description = "Returns paged report of employees whose revaccination date falls within the next N days",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Report page",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = RevaccinationDueItem::class)),
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
    fun getRevaccinationDue(
        request: HttpServletRequest,
        @Parameter(description = "Number of days from today to include in due window", example = "30")
        @RequestParam
        days: Int,
        @Parameter(description = "Optional department filter", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestParam(required = false)
        departmentId: UUID?,
        @Parameter(description = "Page index (0-based)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
    ): Page<RevaccinationDueItem> {
        if (days < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be >= 0")
        }

        val scope = requireScope(request)

        return service.getDueInDays(
            days = days,
            scope = scope,
            pageable = PageRequest.of(page, size),
        )
    }

    @GetMapping("/revaccination-due/export")
    @Operation(summary = "Export revaccination due report (csv, xlsx, pdf)")
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
    fun exportRevaccinationDue(
        request: HttpServletRequest,
        @RequestParam days: Int,
        @RequestParam(required = false) departmentId: UUID?,
        @RequestParam(defaultValue = "csv") format: String,
    ): ResponseEntity<ByteArray> {
        if (days < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be >= 0")
        }
        val reportFormat =
            ReportFormat.fromRaw(format)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported export format")
        val scope = requireScope(request)
        val rows = service.getDueInDaysForExport(days = days, scope = scope)
        val exportView = ReportExportViewModels.revaccinationDue(rows, resolveExportLocale(request))
        val reportFile =
            reportExportService.export(
                format = reportFormat,
                fileNameBase = "revaccination-due",
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

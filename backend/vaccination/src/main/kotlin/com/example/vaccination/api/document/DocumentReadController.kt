package com.example.vaccination.api.document

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.vaccination.api.read.VaccinationReadService
import com.example.vaccination.api.security.VaccinationSecurityContext
import com.example.vaccination.document.DocumentEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@RestController
@Tag(name = "Documents", description = "Read operations for vaccination documents")
class DocumentReadController(
    private val readService: VaccinationReadService,
) {
    @GetMapping("/vaccinations/{vaccinationId}/documents")
    @Operation(summary = "Get documents by vaccination id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Documents list"),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Vaccination not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun listByVaccination(
        request: HttpServletRequest,
        @PathVariable vaccinationId: UUID,
    ): List<DocumentReadResponse> =
        readService
            .listVaccinationDocuments(
                principal = requirePrincipal(request),
                vaccinationId = vaccinationId,
            ).map(DocumentReadResponse::fromEntity)

    @GetMapping("/documents/{id}")
    @Operation(summary = "Get document by id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Document metadata"),
            ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Document not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun getById(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ): DocumentReadResponse =
        DocumentReadResponse.fromEntity(
            readService.getDocument(
                principal = requirePrincipal(request),
                documentId = id,
            ),
        )

    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(VaccinationSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

data class DocumentReadResponse(
    val id: UUID,
    val vaccinationId: UUID,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedBy: UUID,
    val uploadedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: DocumentEntity): DocumentReadResponse =
            DocumentReadResponse(
                id = entity.id!!,
                vaccinationId = entity.vaccinationId!!,
                fileName = entity.fileName,
                filePath = entity.filePath,
                fileSize = entity.fileSize,
                mimeType = entity.mimeType,
                uploadedBy = entity.uploadedBy!!,
                uploadedAt = entity.uploadedAt!!,
            )
    }
}

package com.example.vaccination.api.document

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.vaccination.api.read.VaccinationReadService
import com.example.vaccination.api.security.VaccinationSecurityContext
import com.example.vaccination.api.security.VaccinationWriteScopeService
import com.example.vaccination.document.DocumentContentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "Document content operations")
class DocumentContentController(
    private val documentContentService: DocumentContentService,
    private val readService: VaccinationReadService,
    private val writeScopeService: VaccinationWriteScopeService,
) {
    @PostMapping("/{id}/content")
    @Operation(summary = "Upload document content")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Document content uploaded"),
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
                description = "Document not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun upload(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @RequestPart("file") file: MultipartFile,
    ): DocumentReadResponse {
        val principal = requirePrincipal(request)
        writeScopeService.assertDocumentContentWriteAllowed(principal, id)
        val updated =
            documentContentService.uploadContent(
                documentId = id,
                originalFileName = file.originalFilename,
                contentType = file.contentType,
                bytes = file.bytes,
            )
        return DocumentReadResponse.fromEntity(updated)
    }

    @GetMapping("/{id}/content")
    @Operation(summary = "Download document content")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Document content stream"),
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
                description = "Document or content not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun download(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ): ResponseEntity<ByteArrayResource> {
        val principal = requirePrincipal(request)
        // read scope check
        readService.getDocument(principal, id)
        val content = documentContentService.downloadContent(id)

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(content.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${content.fileName}\"")
            .body(ByteArrayResource(content.bytes))
    }

    @DeleteMapping("/{id}/content")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete document content from storage")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Document content deleted"),
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
                description = "Document or content not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun delete(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ) {
        val principal = requirePrincipal(request)
        writeScopeService.assertDocumentContentWriteAllowed(principal, id)
        documentContentService.deleteContent(id)
    }

    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(VaccinationSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

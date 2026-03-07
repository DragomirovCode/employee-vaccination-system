package com.example.vaccination.api.document

import com.example.auth.AuthenticatedPrincipal
import com.example.vaccination.api.security.VaccinationSecurityContext
import com.example.vaccination.document.CreateDocumentCommand
import com.example.vaccination.document.DocumentService
import com.example.vaccination.document.UpdateDocumentCommand
import io.swagger.v3.oas.annotations.Operation
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
import java.util.UUID

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "Write operations for vaccination documents")
class DocumentWriteController(
    private val documentService: DocumentService,
) {
    @PostMapping
    @Operation(summary = "Create document metadata")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Document created"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
        ],
    )
    fun create(
        request: HttpServletRequest,
        @RequestBody body: DocumentWriteRequest,
    ): DocumentWriteResponse {
        val principal = requirePrincipal(request)
        val created =
            documentService.create(
                CreateDocumentCommand(
                    vaccinationId = body.vaccinationId,
                    fileName = body.fileName,
                    filePath = body.filePath,
                    fileSize = body.fileSize,
                    mimeType = body.mimeType,
                    uploadedBy = principal.userId,
                ),
            )

        return DocumentWriteResponse(id = created.id!!)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update document metadata")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Document updated"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
        ],
    )
    fun update(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @RequestBody body: DocumentWriteRequest,
    ): DocumentWriteResponse {
        val principal = requirePrincipal(request)
        val updated =
            documentService.update(
                id = id,
                command =
                    UpdateDocumentCommand(
                        vaccinationId = body.vaccinationId,
                        fileName = body.fileName,
                        filePath = body.filePath,
                        fileSize = body.fileSize,
                        mimeType = body.mimeType,
                        modifiedBy = principal.userId,
                    ),
            )

        return DocumentWriteResponse(id = updated.id!!)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete document metadata")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Document deleted"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Forbidden"),
        ],
    )
    fun delete(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ) {
        val principal = requirePrincipal(request)
        documentService.delete(id = id, deletedBy = principal.userId)
    }

    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(VaccinationSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

data class DocumentWriteRequest(
    val vaccinationId: UUID,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
)

data class DocumentWriteResponse(
    val id: UUID,
)

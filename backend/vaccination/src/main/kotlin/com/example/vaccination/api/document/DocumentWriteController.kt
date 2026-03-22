package com.example.vaccination.api.document

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.vaccination.api.security.VaccinationSecurityContext
import com.example.vaccination.api.security.VaccinationWriteScopeService
import com.example.vaccination.document.CreateDocumentCommand
import com.example.vaccination.document.DocumentService
import com.example.vaccination.document.UpdateDocumentCommand
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
import java.util.UUID

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "Write operations for vaccination documents")
class DocumentWriteController(
    private val documentService: DocumentService,
    private val scopeService: VaccinationWriteScopeService,
) {
    /** Создает метаданные документа вакцинации. */
    @PostMapping
    @Operation(summary = "Create document metadata")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Document created"),
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
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun create(
        request: HttpServletRequest,
        @RequestBody body: DocumentWriteRequest,
    ): DocumentWriteResponse {
        val principal = requirePrincipal(request)
        scopeService.assertDocumentCreateAllowed(principal, body.vaccinationId)
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

    /** Обновляет метаданные документа вакцинации. */
    @PutMapping("/{id}")
    @Operation(summary = "Update document metadata")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Document updated"),
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
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun update(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @RequestBody body: DocumentWriteRequest,
    ): DocumentWriteResponse {
        val principal = requirePrincipal(request)
        scopeService.assertDocumentUpdateAllowed(principal, id, body.vaccinationId)
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

    /** Удаляет метаданные документа вакцинации. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete document metadata")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Document deleted"),
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
    fun delete(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ) {
        val principal = requirePrincipal(request)
        scopeService.assertDocumentDeleteAllowed(principal, id)
        documentService.delete(id = id, deletedBy = principal.userId)
    }

    /**
     * Извлекает аутентифицированного пользователя из атрибутов запроса.
     */
    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(VaccinationSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

/**
 * Тело запроса на создание или обновление метаданных документа.
 */
data class DocumentWriteRequest(
    /** Идентификатор записи вакцинации. */
    val vaccinationId: UUID,
    /** Имя файла. */
    val fileName: String,
    /** Путь или ключ объекта в хранилище. */
    val filePath: String,
    /** Размер файла в байтах. */
    val fileSize: Long,
    /** MIME-тип файла. */
    val mimeType: String,
)

/**
 * Ответ на успешную операцию с метаданными документа.
 */
data class DocumentWriteResponse(
    /** Идентификатор документа. */
    val id: UUID,
)

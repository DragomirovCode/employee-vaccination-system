package com.example.vaccination.document

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val auditLogService: AuditLogService,
) {
    @Transactional
    fun create(command: CreateDocumentCommand): DocumentEntity {
        val saved =
            documentRepository.saveAndFlush(
                DocumentEntity(
                    vaccinationId = command.vaccinationId,
                    fileName = command.fileName,
                    filePath = command.filePath,
                    fileSize = command.fileSize,
                    mimeType = command.mimeType,
                    uploadedBy = command.uploadedBy,
                ),
            )

        auditLogService.logCreate(
            userId = command.uploadedBy,
            entityType = AuditEntityType.DOCUMENT,
            entityId = saved.id!!,
            newValue = saved.toAuditPayload(),
        )

        return saved
    }

    @Transactional
    fun update(
        id: UUID,
        command: UpdateDocumentCommand,
    ): DocumentEntity {
        val existing =
            documentRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Document not found: $id") }

        val oldPayload = existing.toAuditPayload()
        existing.vaccinationId = command.vaccinationId
        existing.fileName = command.fileName
        existing.filePath = command.filePath
        existing.fileSize = command.fileSize
        existing.mimeType = command.mimeType

        val saved = documentRepository.saveAndFlush(existing)
        auditLogService.logUpdate(
            userId = command.modifiedBy,
            entityType = AuditEntityType.DOCUMENT,
            entityId = saved.id!!,
            oldValue = oldPayload,
            newValue = saved.toAuditPayload(),
        )

        return saved
    }

    @Transactional
    fun delete(
        id: UUID,
        deletedBy: UUID,
    ) {
        val existing =
            documentRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Document not found: $id") }

        val oldPayload = existing.toAuditPayload()
        documentRepository.delete(existing)
        auditLogService.logDelete(
            userId = deletedBy,
            entityType = AuditEntityType.DOCUMENT,
            entityId = id,
            oldValue = oldPayload,
        )
    }

    private fun DocumentEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "id" to id?.toString(),
            "vaccinationId" to vaccinationId?.toString(),
            "fileName" to fileName,
            "filePath" to filePath,
            "fileSize" to fileSize,
            "mimeType" to mimeType,
            "uploadedBy" to uploadedBy?.toString(),
            "uploadedAt" to uploadedAt?.toString(),
        )
}

data class CreateDocumentCommand(
    val vaccinationId: UUID,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedBy: UUID,
)

data class UpdateDocumentCommand(
    val vaccinationId: UUID,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val modifiedBy: UUID,
)

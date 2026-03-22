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
    /**
     * Создает запись о документе и фиксирует операцию в журнале аудита.
     */
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

    /**
     * Обновляет метаданные документа и сохраняет изменения в аудите.
     */
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

    /**
     * Удаляет запись о документе и сохраняет удаление в журнале аудита.
     */
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

    /**
     * Преобразует документ в сериализуемое представление для аудита.
     */
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

/**
 * Команда создания метаданных документа.
 */
data class CreateDocumentCommand(
    /** Идентификатор записи вакцинации. */
    val vaccinationId: UUID,
    /** Имя файла. */
    val fileName: String,
    /** Путь или ключ файла в хранилище. */
    val filePath: String,
    /** Размер файла в байтах. */
    val fileSize: Long,
    /** MIME-тип файла. */
    val mimeType: String,
    /** Идентификатор пользователя, загружающего документ. */
    val uploadedBy: UUID,
)

/**
 * Команда обновления метаданных документа.
 */
data class UpdateDocumentCommand(
    /** Идентификатор записи вакцинации. */
    val vaccinationId: UUID,
    /** Имя файла. */
    val fileName: String,
    /** Путь или ключ файла в хранилище. */
    val filePath: String,
    /** Размер файла в байтах. */
    val fileSize: Long,
    /** MIME-тип файла. */
    val mimeType: String,
    /** Идентификатор пользователя, выполняющего изменение. */
    val modifiedBy: UUID,
)

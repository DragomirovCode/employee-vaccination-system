package com.example.vaccination.document

import com.example.vaccination.storage.DocumentContentStorage
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class DocumentContentService(
    private val documentRepository: DocumentRepository,
    private val contentStorage: DocumentContentStorage,
) {
    /**
     * Загружает бинарное содержимое документа в хранилище и обновляет его метаданные.
     */
    @Transactional
    fun uploadContent(
        documentId: UUID,
        originalFileName: String?,
        contentType: String?,
        bytes: ByteArray,
    ): DocumentEntity {
        if (bytes.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        }

        val document =
            documentRepository.findById(documentId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
            }

        val safeFileName = sanitizeFileName(originalFileName ?: document.fileName)
        val objectKey = "documents/$documentId/$safeFileName"
        val effectiveContentType = contentType ?: document.mimeType

        contentStorage.put(objectKey = objectKey, contentType = effectiveContentType, bytes = bytes)

        document.fileName = safeFileName
        document.filePath = objectKey
        document.fileSize = bytes.size.toLong()
        document.mimeType = effectiveContentType
        return documentRepository.saveAndFlush(document)
    }

    /**
     * Загружает содержимое документа из хранилища вместе с его метаданными.
     */
    @Transactional(readOnly = true)
    fun downloadContent(documentId: UUID): DownloadedDocumentContent {
        val document =
            documentRepository.findById(documentId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
            }

        val content =
            contentStorage.get(document.filePath)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document content not found")

        return DownloadedDocumentContent(
            fileName = document.fileName,
            contentType = content.contentType ?: document.mimeType,
            bytes = content.bytes,
        )
    }

    /**
     * Удаляет бинарное содержимое документа из хранилища.
     */
    @Transactional
    fun deleteContent(documentId: UUID) {
        val document =
            documentRepository.findById(documentId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
            }
        val removed = contentStorage.delete(document.filePath)
        if (!removed) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document content not found")
        }
    }

    /**
     * Заменяет недопустимые символы в имени файла на безопасные.
     */
    private fun sanitizeFileName(name: String): String = name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
}

/**
 * Данные, необходимые для скачивания содержимого документа.
 */
data class DownloadedDocumentContent(
    /** Имя файла. */
    val fileName: String,
    /** MIME-тип содержимого. */
    val contentType: String,
    /** Бинарное содержимое файла. */
    val bytes: ByteArray,
)

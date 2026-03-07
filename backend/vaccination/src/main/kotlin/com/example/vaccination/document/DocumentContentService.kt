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

    private fun sanitizeFileName(name: String): String = name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
}

data class DownloadedDocumentContent(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
)

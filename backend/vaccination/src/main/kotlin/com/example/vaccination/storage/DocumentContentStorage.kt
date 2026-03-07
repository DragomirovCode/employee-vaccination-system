package com.example.vaccination.storage

data class StoredDocumentContent(
    val bytes: ByteArray,
    val contentType: String?,
)

interface DocumentContentStorage {
    fun put(
        objectKey: String,
        contentType: String?,
        bytes: ByteArray,
    )

    fun get(objectKey: String): StoredDocumentContent?

    fun delete(objectKey: String): Boolean
}

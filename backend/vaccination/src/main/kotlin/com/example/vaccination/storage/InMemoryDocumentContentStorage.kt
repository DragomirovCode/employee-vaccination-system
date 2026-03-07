package com.example.vaccination.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@ConditionalOnProperty(name = ["storage.provider"], havingValue = "inmemory", matchIfMissing = true)
class InMemoryDocumentContentStorage : DocumentContentStorage {
    private val storage = ConcurrentHashMap<String, StoredDocumentContent>()

    override fun put(
        objectKey: String,
        contentType: String?,
        bytes: ByteArray,
    ) {
        storage[objectKey] = StoredDocumentContent(bytes = bytes, contentType = contentType)
    }

    override fun get(objectKey: String): StoredDocumentContent? = storage[objectKey]

    override fun delete(objectKey: String): Boolean = storage.remove(objectKey) != null
}

package com.example.vaccination.storage

/**
 * Содержимое документа, считанное из файлового хранилища.
 */
data class StoredDocumentContent(
    /** Бинарное содержимое файла. */
    val bytes: ByteArray,
    /** MIME-тип содержимого, если он известен. */
    val contentType: String?,
)

/**
 * Абстракция файлового хранилища для содержимого документов.
 */
interface DocumentContentStorage {
    /**
     * Сохраняет содержимое документа по указанному ключу.
     */
    fun put(
        objectKey: String,
        contentType: String?,
        bytes: ByteArray,
    )

    /**
     * Возвращает содержимое документа по ключу либо `null`, если объект не найден.
     */
    fun get(objectKey: String): StoredDocumentContent?

    /**
     * Удаляет объект по ключу.
     *
     * @return `true`, если объект был удален
     */
    fun delete(objectKey: String): Boolean
}

package com.example.vaccination.storage

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Свойства подключения к MinIO-хранилищу документов.
 */
@ConfigurationProperties(prefix = "storage.minio")
data class MinioStorageProperties(
    /** URL MinIO-сервера. */
    var endpoint: String = "http://localhost:9000",
    /** Ключ доступа. */
    var accessKey: String = "minioadmin",
    /** Секретный ключ. */
    var secretKey: String = "minioadmin",
    /** Имя bucket для документов. */
    var bucket: String = "evs-documents",
)

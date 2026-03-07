package com.example.vaccination.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage.minio")
data class MinioStorageProperties(
    var endpoint: String = "http://localhost:9000",
    var accessKey: String = "minioadmin",
    var secretKey: String = "minioadmin",
    var bucket: String = "evs-documents",
)

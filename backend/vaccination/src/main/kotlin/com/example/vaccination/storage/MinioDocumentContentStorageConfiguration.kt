package com.example.vaccination.storage

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream

@Configuration
@ConditionalOnProperty(name = ["storage.provider"], havingValue = "minio")
@EnableConfigurationProperties(MinioStorageProperties::class)
class MinioDocumentContentStorageConfiguration {
    @Bean
    fun minioClient(properties: MinioStorageProperties): MinioClient =
        MinioClient
            .builder()
            .endpoint(properties.endpoint)
            .credentials(properties.accessKey, properties.secretKey)
            .build()

    @Bean
    fun minioDocumentContentStorage(
        minioClient: MinioClient,
        properties: MinioStorageProperties,
    ): DocumentContentStorage = MinioDocumentContentStorage(minioClient, properties)
}

private class MinioDocumentContentStorage(
    private val minioClient: MinioClient,
    private val properties: MinioStorageProperties,
) : DocumentContentStorage {
    override fun put(
        objectKey: String,
        contentType: String?,
        bytes: ByteArray,
    ) {
        ensureBucket()
        val stream = ByteArrayInputStream(bytes)
        minioClient.putObject(
            PutObjectArgs
                .builder()
                .bucket(properties.bucket)
                .`object`(objectKey)
                .contentType(contentType ?: "application/octet-stream")
                .stream(stream, bytes.size.toLong(), -1)
                .build(),
        )
    }

    override fun get(objectKey: String): StoredDocumentContent? =
        runCatching {
            minioClient
                .getObject(
                    GetObjectArgs
                        .builder()
                        .bucket(properties.bucket)
                        .`object`(objectKey)
                        .build(),
                ).use { input ->
                    StoredDocumentContent(
                        bytes = input.readAllBytes(),
                        contentType = null,
                    )
                }
        }.getOrNull()

    override fun delete(objectKey: String): Boolean =
        runCatching {
            minioClient.removeObject(
                RemoveObjectArgs
                    .builder()
                    .bucket(properties.bucket)
                    .`object`(objectKey)
                    .build(),
            )
            true
        }.getOrDefault(false)

    private fun ensureBucket() {
        val exists =
            minioClient.bucketExists(
                BucketExistsArgs
                    .builder()
                    .bucket(properties.bucket)
                    .build(),
            )

        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs
                    .builder()
                    .bucket(properties.bucket)
                    .build(),
            )
        }
    }
}

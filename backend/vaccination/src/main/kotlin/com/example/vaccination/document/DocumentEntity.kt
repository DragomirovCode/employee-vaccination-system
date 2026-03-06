package com.example.vaccination.document

import com.example.vaccination.common.UuidV7Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "documents")
class DocumentEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    @Column(name = "vaccination_id", nullable = false)
    var vaccinationId: UUID? = null,
    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String = "",
    @Column(name = "file_path", nullable = false, length = 1024)
    var filePath: String = "",
    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0,
    @Column(name = "mime_type", nullable = false, length = 255)
    var mimeType: String = "",
    @Column(name = "uploaded_by", nullable = false)
    var uploadedBy: UUID? = null,
    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant? = null,
) {
    @PrePersist
    fun onCreate() {
        if (id == null) {
            id = UuidV7Generator.next()
        }
        if (uploadedAt == null) {
            uploadedAt = Instant.now()
        }
    }
}

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
/**
 * JPA-сущность документа, прикрепленного к записи о вакцинации.
 */
class DocumentEntity(
    /** Уникальный идентификатор документа. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Идентификатор записи вакцинации, к которой прикреплен документ. */
    @Column(name = "vaccination_id", nullable = false)
    var vaccinationId: UUID? = null,
    /** Отображаемое имя файла. */
    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String = "",
    /** Путь или ключ объекта в файловом хранилище. */
    @Column(name = "file_path", nullable = false, length = 1024)
    var filePath: String = "",
    /** Размер файла в байтах. */
    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0,
    /** MIME-тип файла. */
    @Column(name = "mime_type", nullable = false, length = 255)
    var mimeType: String = "",
    /** Идентификатор пользователя, загрузившего документ. */
    @Column(name = "uploaded_by", nullable = false)
    var uploadedBy: UUID? = null,
    /** Момент загрузки документа. */
    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant? = null,
) {
    /**
     * Заполняет идентификатор и время загрузки перед первой вставкой в базу данных.
     */
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

package com.example.auth.notification

import com.example.auth.user.UuidV7Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * JPA-сущность пользовательского уведомления.
 */
@Entity
@Table(name = "notifications")
class NotificationEntity(
    /** Уникальный идентификатор уведомления. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Идентификатор пользователя, которому адресовано уведомление. */
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,
    /** Тип уведомления. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    var type: NotificationType = NotificationType.SYSTEM,
    /** Короткий заголовок уведомления. */
    @Column(nullable = false, length = 255)
    var title: String = "",
    /** Основной текст уведомления. */
    @Column(nullable = false, columnDefinition = "TEXT")
    var message: String = "",
    /** Признак того, что уведомление уже прочитано. */
    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
    /** Момент создания уведомления. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    /** Момент, когда пользователь прочитал уведомление. */
    @Column(name = "read_at")
    var readAt: Instant? = null,
    /** Дополнительные данные уведомления в JSON-формате. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: String? = null,
) {
    /**
     * Заполняет системные поля перед первой вставкой уведомления в базу данных.
     */
    @PrePersist
    fun onCreate() {
        if (id == null) {
            id = UuidV7Generator.next()
        }
        if (createdAt == null) {
            createdAt = Instant.now()
        }
    }
}

enum class NotificationType {
    REVACCINATION_DUE,
    REVOKED_DOCUMENT,
    SYSTEM,
}

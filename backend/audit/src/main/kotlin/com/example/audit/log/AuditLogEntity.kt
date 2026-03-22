package com.example.audit.log

import com.example.audit.common.UuidV7Generator
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

@Entity
@Table(name = "audit_log")
/**
 * JPA-сущность записи журнала аудита.
 *
 * Хранит сведения о том, кто, какое действие и над какой бизнес-сущностью выполнил,
 * а также снимки состояния данных до и после изменения.
 */
class AuditLogEntity(
    /** Уникальный идентификатор записи аудита. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Идентификатор пользователя, выполнившего действие. */
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,
    /** Тип операции, зафиксированной в журнале аудита. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var action: AuditAction? = null,
    /** Тип бизнес-сущности, над которой было выполнено действие. */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    var entityType: AuditEntityType? = null,
    /** Идентификатор бизнес-сущности, к которой относится запись аудита. */
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID? = null,
    /** JSON-снимок состояния сущности до изменения. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    var oldValue: String? = null,
    /** JSON-снимок состояния сущности после изменения. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    var newValue: String? = null,
    /** Момент создания записи аудита. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
) {
    /**
     * Подготавливает сущность к сохранению: при необходимости заполняет идентификатор
     * и время создания перед первой вставкой в базу данных.
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

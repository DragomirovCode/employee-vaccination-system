package com.example.audit.log

import com.example.audit.common.UuidV7Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLogEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var action: AuditAction? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    var entityType: AuditEntityType? = null,
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID? = null,
    @Column(name = "old_value", columnDefinition = "TEXT")
    var oldValue: String? = null,
    @Column(name = "new_value", columnDefinition = "TEXT")
    var newValue: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
) {
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

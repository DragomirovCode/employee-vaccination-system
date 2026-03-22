package com.example.employee.department

import com.example.employee.common.UuidV4Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "departments")
/**
 * JPA-сущность подразделения организации.
 */
class DepartmentEntity(
    /** Уникальный идентификатор подразделения. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Наименование подразделения. */
    @Column(nullable = false, length = 255)
    var name: String = "",
    /** Идентификатор родительского подразделения в иерархии. */
    @Column(name = "parent_id")
    var parentId: UUID? = null,
    /** Момент создания подразделения. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
) {
    /**
     * Заполняет идентификатор и дату создания перед первой вставкой в базу данных.
     */
    @PrePersist
    fun onCreate() {
        if (id == null) {
            id = UuidV4Generator.next()
        }
        if (createdAt == null) {
            createdAt = Instant.now()
        }
    }
}

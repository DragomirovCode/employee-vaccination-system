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
class DepartmentEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    @Column(nullable = false, length = 255)
    var name: String = "",
    @Column(name = "parent_id")
    var parentId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
) {
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

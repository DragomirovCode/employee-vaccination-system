package com.example.employee.person

import com.example.employee.common.UuidV7Generator
import com.example.employee.department.DepartmentEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "employees")
class EmployeeEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    @Column(name = "user_id", unique = true)
    var userId: UUID? = null,
    @Column(name = "department_id", nullable = false)
    var departmentId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    var department: DepartmentEntity? = null,
    @Column(name = "first_name", nullable = false, length = 255)
    var firstName: String = "",
    @Column(name = "last_name", nullable = false, length = 255)
    var lastName: String = "",
    @Column(name = "middle_name", length = 255)
    var middleName: String? = null,
    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,
    @Column(name = "position", length = 255)
    var position: String? = null,
    @Column(name = "hire_date")
    var hireDate: LocalDate? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    @PrePersist
    fun onCreate() {
        if (id == null) {
            id = UuidV7Generator.next()
        }
        val now = Instant.now()
        if (createdAt == null) {
            createdAt = now
        }
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

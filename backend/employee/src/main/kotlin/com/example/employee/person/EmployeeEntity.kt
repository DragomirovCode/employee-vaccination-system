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

/**
 * JPA-сущность сотрудника организации.
 */
@Entity
@Table(name = "employees")
class EmployeeEntity(
    /** Уникальный идентификатор сотрудника. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Идентификатор связанной учетной записи пользователя, если она назначена. */
    @Column(name = "user_id", unique = true)
    var userId: UUID? = null,
    /** Идентификатор подразделения, к которому относится сотрудник. */
    @Column(name = "department_id", nullable = false)
    var departmentId: UUID? = null,
    /** Связанное подразделение сотрудника. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    var department: DepartmentEntity? = null,
    /** Имя сотрудника. */
    @Column(name = "first_name", nullable = false, length = 255)
    var firstName: String = "",
    /** Фамилия сотрудника. */
    @Column(name = "last_name", nullable = false, length = 255)
    var lastName: String = "",
    /** Отчество сотрудника. */
    @Column(name = "middle_name", length = 255)
    var middleName: String? = null,
    /** Дата рождения сотрудника. */
    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,
    /** Должность сотрудника. */
    @Column(name = "position", length = 255)
    var position: String? = null,
    /** Дата приема на работу. */
    @Column(name = "hire_date")
    var hireDate: LocalDate? = null,
    /** Момент создания записи сотрудника. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    /** Момент последнего обновления записи сотрудника. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    /**
     * Заполняет идентификатор и временные метки перед первой вставкой сотрудника.
     */
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

    /**
     * Обновляет время последнего изменения записи сотрудника.
     */
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

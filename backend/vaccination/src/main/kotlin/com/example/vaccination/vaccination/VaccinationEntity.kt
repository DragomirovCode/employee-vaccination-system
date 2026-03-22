package com.example.vaccination.vaccination

import com.example.vaccination.common.UuidV7Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * JPA-сущность записи о вакцинации сотрудника.
 */
@Entity
@Table(name = "vaccinations")
class VaccinationEntity(
    /** Уникальный идентификатор записи вакцинации. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Идентификатор вакцинируемого сотрудника. */
    @Column(name = "employee_id", nullable = false)
    var employeeId: UUID? = null,
    /** Идентификатор вакцины. */
    @Column(name = "vaccine_id", nullable = false)
    var vaccineId: UUID? = null,
    /** Идентификатор пользователя, зафиксировавшего вакцинацию. */
    @Column(name = "performed_by", nullable = false)
    var performedBy: UUID? = null,
    /** Дата проведения вакцинации. */
    @Column(name = "vaccination_date", nullable = false)
    var vaccinationDate: LocalDate? = null,
    /** Номер дозы в схеме вакцинации. */
    @Column(name = "dose_number", nullable = false)
    var doseNumber: Int = 1,
    /** Номер партии препарата. */
    @Column(name = "batch_number", length = 255)
    var batchNumber: String? = null,
    /** Срок годности использованной дозы. */
    @Column(name = "expiration_date", nullable = false)
    var expirationDate: LocalDate? = null,
    /** Дата следующей дозы, если она требуется. */
    @Column(name = "next_dose_date")
    var nextDoseDate: LocalDate? = null,
    /** Дата ревакцинации. */
    @Column(name = "revaccination_date")
    var revaccinationDate: LocalDate? = null,
    /** Дополнительные заметки по записи вакцинации. */
    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,
    /** Момент создания записи. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    /** Момент последнего обновления записи. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    /**
     * Заполняет идентификатор и временные метки перед первой вставкой в базу данных.
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
     * Обновляет время последнего изменения записи вакцинации.
     */
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

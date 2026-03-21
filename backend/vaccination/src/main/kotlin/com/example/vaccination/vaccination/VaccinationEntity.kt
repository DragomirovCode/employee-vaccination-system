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

@Entity
@Table(name = "vaccinations")
class VaccinationEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    @Column(name = "employee_id", nullable = false)
    var employeeId: UUID? = null,
    @Column(name = "vaccine_id", nullable = false)
    var vaccineId: UUID? = null,
    @Column(name = "performed_by", nullable = false)
    var performedBy: UUID? = null,
    @Column(name = "vaccination_date", nullable = false)
    var vaccinationDate: LocalDate? = null,
    @Column(name = "dose_number", nullable = false)
    var doseNumber: Int = 1,
    @Column(name = "batch_number", length = 255)
    var batchNumber: String? = null,
    @Column(name = "expiration_date", nullable = false)
    var expirationDate: LocalDate? = null,
    @Column(name = "next_dose_date")
    var nextDoseDate: LocalDate? = null,
    @Column(name = "revaccination_date")
    var revaccinationDate: LocalDate? = null,
    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,
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

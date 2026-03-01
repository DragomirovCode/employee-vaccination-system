package com.example.vaccine.vaccine

import com.example.vaccine.common.UuidV4Generator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "vaccines")
class VaccineEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    @Column(nullable = false, length = 255)
    var name: String = "",
    @Column(length = 255)
    var manufacturer: String? = null,
    @Column(name = "validity_days", nullable = false)
    var validityDays: Int = 0,
    @Column(name = "doses_required", nullable = false)
    var dosesRequired: Int = 1,
    @Column(name = "days_between")
    var daysBetween: Int? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
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

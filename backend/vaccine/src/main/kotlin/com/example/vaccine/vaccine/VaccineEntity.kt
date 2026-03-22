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
/**
 * JPA-сущность вакцины.
 */
class VaccineEntity(
    /** Уникальный идентификатор вакцины. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Наименование вакцины. */
    @Column(nullable = false, length = 255)
    var name: String = "",
    /** Производитель вакцины. */
    @Column(length = 255)
    var manufacturer: String? = null,
    /** Срок действия вакцинации в днях. */
    @Column(name = "validity_days", nullable = false)
    var validityDays: Int = 0,
    /** Требуемое количество доз. */
    @Column(name = "doses_required", nullable = false)
    var dosesRequired: Int = 1,
    /** Интервал между дозами в днях. */
    @Column(name = "days_between")
    var daysBetween: Int? = null,
    /** Признак активности вакцины в справочнике. */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    /** Момент создания записи. */
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

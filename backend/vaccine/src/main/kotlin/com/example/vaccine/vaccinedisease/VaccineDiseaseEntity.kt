package com.example.vaccine.vaccinedisease

import com.example.vaccine.disease.DiseaseEntity
import com.example.vaccine.vaccine.VaccineEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * JPA-сущность связи вакцины с заболеванием.
 */
@Entity
@Table(name = "vaccine_diseases")
class VaccineDiseaseEntity(
    /** Составной идентификатор связи. */
    @EmbeddedId
    var id: VaccineDiseaseId = VaccineDiseaseId(),
    /** Связанная вакцина. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", insertable = false, updatable = false)
    var vaccine: VaccineEntity? = null,
    /** Связанное заболевание. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_id", insertable = false, updatable = false)
    var disease: DiseaseEntity? = null,
)

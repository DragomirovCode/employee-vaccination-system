package com.example.vaccine.vaccinedisease

import com.example.vaccine.disease.DiseaseEntity
import com.example.vaccine.vaccine.VaccineEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "vaccine_diseases")
class VaccineDiseaseEntity(
    @EmbeddedId
    var id: VaccineDiseaseId = VaccineDiseaseId(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", insertable = false, updatable = false)
    var vaccine: VaccineEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_id", insertable = false, updatable = false)
    var disease: DiseaseEntity? = null,
)

package com.example.vaccine.vaccinedisease

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class VaccineDiseaseId(
    @Column(name = "vaccine_id", nullable = false)
    var vaccineId: UUID? = null,
    @Column(name = "disease_id", nullable = false)
    var diseaseId: Int? = null,
) : Serializable

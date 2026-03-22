package com.example.vaccine.vaccinedisease

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

/**
 * Составной идентификатор связи вакцины и заболевания.
 */
@Embeddable
data class VaccineDiseaseId(
    /** Идентификатор вакцины. */
    @Column(name = "vaccine_id", nullable = false)
    var vaccineId: UUID? = null,
    /** Идентификатор заболевания. */
    @Column(name = "disease_id", nullable = false)
    var diseaseId: Int? = null,
) : Serializable

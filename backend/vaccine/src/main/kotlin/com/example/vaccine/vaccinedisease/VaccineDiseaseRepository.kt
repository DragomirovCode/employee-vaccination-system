package com.example.vaccine.vaccinedisease

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VaccineDiseaseRepository : JpaRepository<VaccineDiseaseEntity, VaccineDiseaseId> {
    fun findAllByIdVaccineId(vaccineId: UUID): List<VaccineDiseaseEntity>

    fun existsByIdVaccineId(vaccineId: UUID): Boolean

    fun existsByIdDiseaseId(diseaseId: Int): Boolean

    fun existsByIdVaccineIdAndIdDiseaseId(
        vaccineId: UUID,
        diseaseId: Int,
    ): Boolean

    fun deleteByIdVaccineIdAndIdDiseaseId(
        vaccineId: UUID,
        diseaseId: Int,
    ): Long
}

package com.example.vaccine.vaccinedisease

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VaccineDiseaseRepository : JpaRepository<VaccineDiseaseEntity, VaccineDiseaseId> {
    fun findAllByIdVaccineId(vaccineId: UUID): List<VaccineDiseaseEntity>
}

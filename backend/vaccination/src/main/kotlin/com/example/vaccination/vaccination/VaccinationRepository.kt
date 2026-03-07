package com.example.vaccination.vaccination

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDate
import java.util.UUID

interface VaccinationRepository : JpaRepository<VaccinationEntity, UUID>, JpaSpecificationExecutor<VaccinationEntity> {
    fun findAllByEmployeeId(employeeId: UUID): List<VaccinationEntity>

    fun findAllByRevaccinationDateLessThanEqual(revaccinationDate: LocalDate): List<VaccinationEntity>
}

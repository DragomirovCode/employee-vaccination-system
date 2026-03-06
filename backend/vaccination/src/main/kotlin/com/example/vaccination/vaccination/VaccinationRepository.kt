package com.example.vaccination.vaccination

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface VaccinationRepository : JpaRepository<VaccinationEntity, UUID> {
    fun findAllByEmployeeId(employeeId: UUID): List<VaccinationEntity>

    fun findAllByRevaccinationDateLessThanEqual(revaccinationDate: LocalDate): List<VaccinationEntity>
}

package com.example.vaccination.vaccination

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDate
import java.util.UUID

interface VaccinationRepository :
    JpaRepository<VaccinationEntity, UUID>,
    JpaSpecificationExecutor<VaccinationEntity> {
    /**
     * Возвращает все записи вакцинации сотрудника.
     */
    fun findAllByEmployeeId(employeeId: UUID): List<VaccinationEntity>

    /**
     * Возвращает вакцинации, дата ревакцинации по которым наступает не позднее указанной даты.
     */
    fun findAllByRevaccinationDateLessThanEqual(revaccinationDate: LocalDate): List<VaccinationEntity>
}

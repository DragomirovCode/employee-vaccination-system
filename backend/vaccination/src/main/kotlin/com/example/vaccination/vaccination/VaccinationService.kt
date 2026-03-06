package com.example.vaccination.vaccination

import com.example.vaccine.vaccine.VaccineRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class VaccinationService(
    private val vaccinationRepository: VaccinationRepository,
    private val vaccineRepository: VaccineRepository,
) {
    @Transactional
    fun create(command: CreateVaccinationCommand): VaccinationEntity {
        val vaccine =
            vaccineRepository
                .findById(command.vaccineId)
                .orElseThrow { IllegalArgumentException("Vaccine not found: ${command.vaccineId}") }

        val nextDoseDate =
            if (command.doseNumber < vaccine.dosesRequired && vaccine.daysBetween != null) {
                command.vaccinationDate.plusDays(vaccine.daysBetween!!.toLong())
            } else {
                null
            }

        val revaccinationDate = command.vaccinationDate.plusDays(vaccine.validityDays.toLong())

        return vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = command.employeeId,
                vaccineId = command.vaccineId,
                performedBy = command.performedBy,
                vaccinationDate = command.vaccinationDate,
                doseNumber = command.doseNumber,
                batchNumber = command.batchNumber,
                expirationDate = command.expirationDate,
                nextDoseDate = nextDoseDate,
                revaccinationDate = revaccinationDate,
                notes = command.notes,
            ),
        )
    }
}

data class CreateVaccinationCommand(
    val employeeId: UUID,
    val vaccineId: UUID,
    val performedBy: UUID,
    val vaccinationDate: LocalDate,
    val doseNumber: Int,
    val batchNumber: String? = null,
    val expirationDate: LocalDate? = null,
    val notes: String? = null,
)

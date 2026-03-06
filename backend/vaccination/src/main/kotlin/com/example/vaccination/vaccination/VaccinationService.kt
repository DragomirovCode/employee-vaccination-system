package com.example.vaccination.vaccination

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
import com.example.vaccine.vaccine.VaccineRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class VaccinationService(
    private val vaccinationRepository: VaccinationRepository,
    private val vaccineRepository: VaccineRepository,
    private val auditLogService: AuditLogService,
) {
    @Transactional
    fun create(command: CreateVaccinationCommand): VaccinationEntity {
        val computedDates =
            computeDates(
                vaccineId = command.vaccineId,
                vaccinationDate = command.vaccinationDate,
                doseNumber = command.doseNumber,
            )

        val saved =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = command.employeeId,
                    vaccineId = command.vaccineId,
                    performedBy = command.performedBy,
                    vaccinationDate = command.vaccinationDate,
                    doseNumber = command.doseNumber,
                    batchNumber = command.batchNumber,
                    expirationDate = command.expirationDate,
                    nextDoseDate = computedDates.nextDoseDate,
                    revaccinationDate = computedDates.revaccinationDate,
                    notes = command.notes,
                ),
            )

        auditLogService.logCreate(
            userId = command.performedBy,
            entityType = AuditEntityType.VACCINATION,
            entityId = saved.id!!,
            newValue = saved.toAuditPayload(),
        )

        return saved
    }

    @Transactional
    fun update(
        id: UUID,
        command: UpdateVaccinationCommand,
    ): VaccinationEntity {
        val existing =
            vaccinationRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Vaccination not found: $id") }

        val oldPayload = existing.toAuditPayload()
        val computedDates =
            computeDates(
                vaccineId = command.vaccineId,
                vaccinationDate = command.vaccinationDate,
                doseNumber = command.doseNumber,
            )

        existing.employeeId = command.employeeId
        existing.vaccineId = command.vaccineId
        existing.performedBy = command.performedBy
        existing.vaccinationDate = command.vaccinationDate
        existing.doseNumber = command.doseNumber
        existing.batchNumber = command.batchNumber
        existing.expirationDate = command.expirationDate
        existing.nextDoseDate = computedDates.nextDoseDate
        existing.revaccinationDate = computedDates.revaccinationDate
        existing.notes = command.notes

        val saved = vaccinationRepository.saveAndFlush(existing)
        auditLogService.logUpdate(
            userId = command.performedBy,
            entityType = AuditEntityType.VACCINATION,
            entityId = saved.id!!,
            oldValue = oldPayload,
            newValue = saved.toAuditPayload(),
        )

        return saved
    }

    @Transactional
    fun delete(
        id: UUID,
        deletedBy: UUID,
    ) {
        val existing =
            vaccinationRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Vaccination not found: $id") }

        val oldPayload = existing.toAuditPayload()
        vaccinationRepository.delete(existing)

        auditLogService.logDelete(
            userId = deletedBy,
            entityType = AuditEntityType.VACCINATION,
            entityId = id,
            oldValue = oldPayload,
        )
    }

    private fun computeDates(
        vaccineId: UUID,
        vaccinationDate: LocalDate,
        doseNumber: Int,
    ): VaccinationDates {
        val vaccine =
            vaccineRepository
                .findById(vaccineId)
                .orElseThrow { IllegalArgumentException("Vaccine not found: $vaccineId") }

        val nextDoseDate =
            if (doseNumber < vaccine.dosesRequired && vaccine.daysBetween != null) {
                vaccinationDate.plusDays(vaccine.daysBetween!!.toLong())
            } else {
                null
            }

        val revaccinationDate = vaccinationDate.plusDays(vaccine.validityDays.toLong())
        return VaccinationDates(nextDoseDate = nextDoseDate, revaccinationDate = revaccinationDate)
    }

    private fun VaccinationEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "id" to id?.toString(),
            "employeeId" to employeeId?.toString(),
            "vaccineId" to vaccineId?.toString(),
            "performedBy" to performedBy?.toString(),
            "vaccinationDate" to vaccinationDate?.toString(),
            "doseNumber" to doseNumber,
            "batchNumber" to batchNumber,
            "expirationDate" to expirationDate?.toString(),
            "nextDoseDate" to nextDoseDate?.toString(),
            "revaccinationDate" to revaccinationDate?.toString(),
            "notes" to notes,
        )
}

private data class VaccinationDates(
    val nextDoseDate: LocalDate?,
    val revaccinationDate: LocalDate?,
)

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

data class UpdateVaccinationCommand(
    val employeeId: UUID,
    val vaccineId: UUID,
    val performedBy: UUID,
    val vaccinationDate: LocalDate,
    val doseNumber: Int,
    val batchNumber: String? = null,
    val expirationDate: LocalDate? = null,
    val notes: String? = null,
)

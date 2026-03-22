package com.example.vaccination.vaccination

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
import com.example.auth.notification.CreateNotificationCommand
import com.example.auth.notification.NotificationService
import com.example.auth.notification.NotificationType
import com.example.employee.person.EmployeeRepository
import com.example.vaccination.document.DocumentRepository
import com.example.vaccination.storage.DocumentContentStorage
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
    private val employeeRepository: EmployeeRepository,
    private val notificationService: NotificationService,
    private val documentRepository: DocumentRepository,
    private val documentContentStorage: DocumentContentStorage,
) {
    /**
     * Создает запись о вакцинации, сохраняет ее в аудите и при необходимости создает уведомление сотруднику.
     */
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
        maybeCreateVaccinationNotification(
            employeeId = command.employeeId,
            vaccinationId = saved.id!!,
            vaccinationDate = saved.vaccinationDate!!,
            revaccinationDate = saved.revaccinationDate,
        )

        return saved
    }

    /**
     * Обновляет запись о вакцинации, пересчитывает производные даты и фиксирует изменение в аудите.
     */
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
        maybeCreateVaccinationNotification(
            employeeId = command.employeeId,
            vaccinationId = saved.id!!,
            vaccinationDate = saved.vaccinationDate!!,
            revaccinationDate = saved.revaccinationDate,
        )

        return saved
    }

    /**
     * Удаляет запись о вакцинации вместе с привязанными документами и их содержимым.
     */
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
        documentRepository.findAllByVaccinationId(id).forEach { document ->
            documentContentStorage.delete(document.filePath)
            documentRepository.delete(document)
        }
        vaccinationRepository.delete(existing)

        auditLogService.logDelete(
            userId = deletedBy,
            entityType = AuditEntityType.VACCINATION,
            entityId = id,
            oldValue = oldPayload,
        )
    }

    /**
     * Вычисляет дату следующей дозы и дату ревакцинации по параметрам вакцины.
     */
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

    /**
     * Преобразует запись вакцинации в сериализуемое представление для аудита.
     */
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

    /**
     * Создает персональное уведомление о зафиксированной вакцинации, если у сотрудника есть связанный пользователь.
     */
    private fun maybeCreateVaccinationNotification(
        employeeId: UUID,
        vaccinationId: UUID,
        vaccinationDate: LocalDate,
        revaccinationDate: LocalDate?,
    ) {
        val userId = employeeRepository.findById(employeeId).orElse(null)?.userId ?: return
        val message =
            if (revaccinationDate != null) {
                "Vaccination was recorded on $vaccinationDate. Revaccination is due on $revaccinationDate"
            } else {
                "Vaccination was recorded on $vaccinationDate"
            }
        notificationService.create(
            CreateNotificationCommand(
                userId = userId,
                type = NotificationType.SYSTEM,
                title = "Vaccination recorded",
                message = message,
                payload =
                    """{"vaccinationId":"$vaccinationId","employeeId":"$employeeId","vaccinationDate":"$vaccinationDate","revaccinationDate":"$revaccinationDate"}""",
            ),
        )
    }
}

/**
 * Набор производных дат, вычисляемых для записи вакцинации.
 */
private data class VaccinationDates(
    /** Дата следующей дозы, если она требуется. */
    val nextDoseDate: LocalDate?,
    /** Дата ревакцинации. */
    val revaccinationDate: LocalDate?,
)

/**
 * Команда создания записи о вакцинации.
 */
data class CreateVaccinationCommand(
    /** Идентификатор сотрудника. */
    val employeeId: UUID,
    /** Идентификатор вакцины. */
    val vaccineId: UUID,
    /** Идентификатор пользователя, выполняющего операцию. */
    val performedBy: UUID,
    /** Дата вакцинации. */
    val vaccinationDate: LocalDate,
    /** Номер дозы. */
    val doseNumber: Int,
    /** Номер партии препарата. */
    val batchNumber: String? = null,
    /** Срок годности использованной дозы. */
    val expirationDate: LocalDate,
    /** Дополнительные заметки. */
    val notes: String? = null,
)

/**
 * Команда обновления записи о вакцинации.
 */
data class UpdateVaccinationCommand(
    /** Идентификатор сотрудника. */
    val employeeId: UUID,
    /** Идентификатор вакцины. */
    val vaccineId: UUID,
    /** Идентификатор пользователя, выполняющего операцию. */
    val performedBy: UUID,
    /** Дата вакцинации. */
    val vaccinationDate: LocalDate,
    /** Номер дозы. */
    val doseNumber: Int,
    /** Номер партии препарата. */
    val batchNumber: String? = null,
    /** Срок годности использованной дозы. */
    val expirationDate: LocalDate,
    /** Дополнительные заметки. */
    val notes: String? = null,
)

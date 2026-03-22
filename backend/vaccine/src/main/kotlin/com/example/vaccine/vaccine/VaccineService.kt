package com.example.vaccine.vaccine

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
import com.example.vaccine.vaccinedisease.VaccineDiseaseRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class VaccineService(
    private val vaccineRepository: VaccineRepository,
    private val vaccineDiseaseRepository: VaccineDiseaseRepository,
    private val auditLogService: AuditLogService,
) {
    /**
     * Возвращает список всех вакцин.
     */
    @Transactional(readOnly = true)
    fun list(): List<VaccineEntity> = vaccineRepository.findAll()

    /**
     * Возвращает вакцину по идентификатору.
     */
    @Transactional(readOnly = true)
    fun get(id: UUID): VaccineEntity = findVaccine(id)

    /**
     * Создает вакцину и фиксирует операцию в журнале аудита.
     */
    @Transactional
    fun create(
        command: CreateVaccineCommand,
        performedBy: UUID,
    ): VaccineEntity {
        requireUniqueName(command.name, null)
        val saved =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = command.name.trim(),
                    manufacturer = command.manufacturer?.trim(),
                    validityDays = command.validityDays,
                    dosesRequired = command.dosesRequired,
                    daysBetween = command.daysBetween,
                    isActive = command.isActive,
                ),
            )
        auditLogService.logCreate(
            userId = performedBy,
            entityType = AuditEntityType.VACCINE,
            entityId = saved.id!!,
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    /**
     * Обновляет вакцину и сохраняет изменения в аудите.
     */
    @Transactional
    fun update(
        id: UUID,
        command: UpdateVaccineCommand,
        performedBy: UUID,
    ): VaccineEntity {
        val vaccine = findVaccine(id)
        val oldPayload = vaccine.toAuditPayload()
        if (vaccineDiseaseRepository.existsVaccinationByVaccineId(id) && !canUpdateUsedVaccine(vaccine, command)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Used vaccine can only change active status",
            )
        }
        requireUniqueName(command.name, id)
        vaccine.name = command.name.trim()
        vaccine.manufacturer = command.manufacturer?.trim()
        vaccine.validityDays = command.validityDays
        vaccine.dosesRequired = command.dosesRequired
        vaccine.daysBetween = command.daysBetween
        vaccine.isActive = command.isActive
        val saved = vaccineRepository.saveAndFlush(vaccine)
        auditLogService.logUpdate(
            userId = performedBy,
            entityType = AuditEntityType.VACCINE,
            entityId = saved.id!!,
            oldValue = oldPayload,
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    /**
     * Удаляет вакцину, если у нее нет связей с заболеваниями и зависимых записей.
     */
    @Transactional
    fun delete(
        id: UUID,
        performedBy: UUID,
    ) {
        val existing = findVaccine(id)
        if (vaccineDiseaseRepository.existsByIdVaccineId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Vaccine has disease links")
        }
        try {
            vaccineRepository.deleteById(id)
            auditLogService.logDelete(
                userId = performedBy,
                entityType = AuditEntityType.VACCINE,
                entityId = id,
                oldValue = existing.toAuditPayload(),
            )
        } catch (ex: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Vaccine has dependent records")
        }
    }

    /**
     * Проверяет уникальность имени вакцины.
     */
    private fun requireUniqueName(
        name: String,
        currentId: UUID?,
    ) {
        val existing = vaccineRepository.findByName(name.trim())
        if (existing != null && existing.id != currentId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Vaccine name already exists")
        }
    }

    /**
     * Ищет вакцину по идентификатору или выбрасывает ошибку 404.
     */
    private fun findVaccine(id: UUID): VaccineEntity =
        vaccineRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine not found")
        }

    /**
     * Проверяет, допустимо ли обновление уже использованной вакцины.
     */
    private fun canUpdateUsedVaccine(
        vaccine: VaccineEntity,
        command: UpdateVaccineCommand,
    ): Boolean =
        vaccine.name == command.name.trim() &&
            vaccine.manufacturer == command.manufacturer?.trim() &&
            vaccine.validityDays == command.validityDays &&
            vaccine.dosesRequired == command.dosesRequired &&
            vaccine.daysBetween == command.daysBetween

    /**
     * Преобразует вакцину в сериализуемое представление для аудита.
     */
    private fun VaccineEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "id" to id?.toString(),
            "name" to name,
            "manufacturer" to manufacturer,
            "validityDays" to validityDays,
            "dosesRequired" to dosesRequired,
            "daysBetween" to daysBetween,
            "isActive" to isActive,
            "createdAt" to createdAt?.toString(),
        )
}

/**
 * Команда создания вакцины.
 */
data class CreateVaccineCommand(
    /** Наименование вакцины. */
    val name: String,
    /** Производитель вакцины. */
    val manufacturer: String?,
    /** Срок действия вакцинации в днях. */
    val validityDays: Int,
    /** Требуемое количество доз. */
    val dosesRequired: Int,
    /** Интервал между дозами в днях. */
    val daysBetween: Int?,
    /** Признак активности вакцины. */
    val isActive: Boolean,
)

/**
 * Команда обновления вакцины.
 */
data class UpdateVaccineCommand(
    /** Наименование вакцины. */
    val name: String,
    /** Производитель вакцины. */
    val manufacturer: String?,
    /** Срок действия вакцинации в днях. */
    val validityDays: Int,
    /** Требуемое количество доз. */
    val dosesRequired: Int,
    /** Интервал между дозами в днях. */
    val daysBetween: Int?,
    /** Признак активности вакцины. */
    val isActive: Boolean,
)

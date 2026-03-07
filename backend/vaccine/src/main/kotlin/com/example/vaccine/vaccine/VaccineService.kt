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
    @Transactional(readOnly = true)
    fun list(): List<VaccineEntity> = vaccineRepository.findAll()

    @Transactional(readOnly = true)
    fun get(id: UUID): VaccineEntity = findVaccine(id)

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

    @Transactional
    fun update(
        id: UUID,
        command: UpdateVaccineCommand,
        performedBy: UUID,
    ): VaccineEntity {
        val vaccine = findVaccine(id)
        val oldPayload = vaccine.toAuditPayload()
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

    private fun requireUniqueName(
        name: String,
        currentId: UUID?,
    ) {
        val existing = vaccineRepository.findByName(name.trim())
        if (existing != null && existing.id != currentId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Vaccine name already exists")
        }
    }

    private fun findVaccine(id: UUID): VaccineEntity =
        vaccineRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine not found")
        }

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

data class CreateVaccineCommand(
    val name: String,
    val manufacturer: String?,
    val validityDays: Int,
    val dosesRequired: Int,
    val daysBetween: Int?,
    val isActive: Boolean,
)

data class UpdateVaccineCommand(
    val name: String,
    val manufacturer: String?,
    val validityDays: Int,
    val dosesRequired: Int,
    val daysBetween: Int?,
    val isActive: Boolean,
)

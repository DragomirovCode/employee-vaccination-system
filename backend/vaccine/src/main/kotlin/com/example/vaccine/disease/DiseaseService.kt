package com.example.vaccine.disease

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
class DiseaseService(
    private val diseaseRepository: DiseaseRepository,
    private val vaccineDiseaseRepository: VaccineDiseaseRepository,
    private val auditLogService: AuditLogService,
) {
    @Transactional(readOnly = true)
    fun list(): List<DiseaseEntity> = diseaseRepository.findAll()

    @Transactional(readOnly = true)
    fun get(id: Int): DiseaseEntity = findDisease(id)

    @Transactional
    fun create(
        command: CreateDiseaseCommand,
        performedBy: UUID,
    ): DiseaseEntity {
        requireUniqueName(command.name, null)
        val saved =
            diseaseRepository.saveAndFlush(
            DiseaseEntity(
                name = command.name.trim(),
                description = command.description?.trim(),
            ),
        )
        auditLogService.logCreate(
            userId = performedBy,
            entityType = AuditEntityType.DISEASE,
            entityKey = saved.id!!.toString(),
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    @Transactional
    fun update(
        id: Int,
        command: UpdateDiseaseCommand,
        performedBy: UUID,
    ): DiseaseEntity {
        val disease = findDisease(id)
        val oldPayload = disease.toAuditPayload()
        requireUniqueName(command.name, id)
        disease.name = command.name.trim()
        disease.description = command.description?.trim()
        val saved = diseaseRepository.saveAndFlush(disease)
        auditLogService.logUpdate(
            userId = performedBy,
            entityType = AuditEntityType.DISEASE,
            entityKey = id.toString(),
            oldValue = oldPayload,
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    @Transactional
    fun delete(
        id: Int,
        performedBy: UUID,
    ) {
        val existing = findDisease(id)
        if (vaccineDiseaseRepository.existsByIdDiseaseId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Disease has vaccine links")
        }
        try {
            diseaseRepository.deleteById(id)
            auditLogService.logDelete(
                userId = performedBy,
                entityType = AuditEntityType.DISEASE,
                entityKey = id.toString(),
                oldValue = existing.toAuditPayload(),
            )
        } catch (ex: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Disease has dependent records")
        }
    }

    private fun requireUniqueName(
        name: String,
        currentId: Int?,
    ) {
        val existing = diseaseRepository.findByName(name.trim())
        if (existing != null && existing.id != currentId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Disease name already exists")
        }
    }

    private fun findDisease(id: Int): DiseaseEntity =
        diseaseRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Disease not found")
        }

    private fun DiseaseEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
        )
}

data class CreateDiseaseCommand(
    val name: String,
    val description: String?,
)

data class UpdateDiseaseCommand(
    val name: String,
    val description: String?,
)

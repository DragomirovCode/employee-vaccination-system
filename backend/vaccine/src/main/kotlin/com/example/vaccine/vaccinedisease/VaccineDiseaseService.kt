package com.example.vaccine.vaccinedisease

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
import com.example.vaccine.disease.DiseaseRepository
import com.example.vaccine.vaccine.VaccineRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class VaccineDiseaseService(
    private val vaccineRepository: VaccineRepository,
    private val diseaseRepository: DiseaseRepository,
    private val vaccineDiseaseRepository: VaccineDiseaseRepository,
    private val auditLogService: AuditLogService,
) {
    /**
     * Возвращает все связи заболевания с указанной вакциной.
     */
    @Transactional(readOnly = true)
    fun listByVaccine(vaccineId: UUID): List<VaccineDiseaseEntity> {
        requireVaccineExists(vaccineId)
        return vaccineDiseaseRepository.findAllByIdVaccineId(vaccineId)
    }

    /**
     * Создает связь вакцины с заболеванием и фиксирует операцию в аудите.
     */
    @Transactional
    fun createLink(
        vaccineId: UUID,
        diseaseId: Int,
        performedBy: UUID,
    ) {
        requireVaccineExists(vaccineId)
        requireDiseaseExists(diseaseId)
        if (vaccineDiseaseRepository.existsByIdVaccineIdAndIdDiseaseId(vaccineId, diseaseId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Vaccine-disease link already exists")
        }
        vaccineDiseaseRepository.saveAndFlush(
            VaccineDiseaseEntity(
                id = VaccineDiseaseId(vaccineId = vaccineId, diseaseId = diseaseId),
            ),
        )
        auditLogService.logCreate(
            userId = performedBy,
            entityType = AuditEntityType.VACCINE_DISEASE,
            entityKey = "$vaccineId:$diseaseId",
            newValue = mapOf("vaccineId" to vaccineId.toString(), "diseaseId" to diseaseId),
        )
    }

    /**
     * Удаляет связь вакцины с заболеванием и фиксирует удаление в аудите.
     */
    @Transactional
    fun deleteLink(
        vaccineId: UUID,
        diseaseId: Int,
        performedBy: UUID,
    ) {
        requireVaccineExists(vaccineId)
        requireDiseaseExists(diseaseId)
        if (vaccineDiseaseRepository.existsVaccinationByVaccineId(vaccineId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Vaccine-disease link cannot be deleted after vaccination usage")
        }
        val deleted = vaccineDiseaseRepository.deleteByIdVaccineIdAndIdDiseaseId(vaccineId, diseaseId)
        if (deleted == 0L) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine-disease link not found")
        }
        auditLogService.logDelete(
            userId = performedBy,
            entityType = AuditEntityType.VACCINE_DISEASE,
            entityKey = "$vaccineId:$diseaseId",
            oldValue = mapOf("vaccineId" to vaccineId.toString(), "diseaseId" to diseaseId),
        )
    }

    /**
     * Проверяет существование вакцины.
     */
    private fun requireVaccineExists(vaccineId: UUID) {
        if (!vaccineRepository.existsById(vaccineId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "vaccineId does not exist")
        }
    }

    /**
     * Проверяет существование заболевания.
     */
    private fun requireDiseaseExists(diseaseId: Int) {
        if (!diseaseRepository.existsById(diseaseId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "diseaseId does not exist")
        }
    }
}

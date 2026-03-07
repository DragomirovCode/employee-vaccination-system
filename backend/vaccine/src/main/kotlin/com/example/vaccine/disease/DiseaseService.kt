package com.example.vaccine.disease

import com.example.vaccine.vaccinedisease.VaccineDiseaseRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class DiseaseService(
    private val diseaseRepository: DiseaseRepository,
    private val vaccineDiseaseRepository: VaccineDiseaseRepository,
) {
    @Transactional(readOnly = true)
    fun list(): List<DiseaseEntity> = diseaseRepository.findAll()

    @Transactional(readOnly = true)
    fun get(id: Int): DiseaseEntity = findDisease(id)

    @Transactional
    fun create(command: CreateDiseaseCommand): DiseaseEntity {
        requireUniqueName(command.name, null)
        return diseaseRepository.saveAndFlush(
            DiseaseEntity(
                name = command.name.trim(),
                description = command.description?.trim(),
            ),
        )
    }

    @Transactional
    fun update(
        id: Int,
        command: UpdateDiseaseCommand,
    ): DiseaseEntity {
        val disease = findDisease(id)
        requireUniqueName(command.name, id)
        disease.name = command.name.trim()
        disease.description = command.description?.trim()
        return diseaseRepository.saveAndFlush(disease)
    }

    @Transactional
    fun delete(id: Int) {
        findDisease(id)
        if (vaccineDiseaseRepository.existsByIdDiseaseId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Disease has vaccine links")
        }
        try {
            diseaseRepository.deleteById(id)
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
}

data class CreateDiseaseCommand(
    val name: String,
    val description: String?,
)

data class UpdateDiseaseCommand(
    val name: String,
    val description: String?,
)

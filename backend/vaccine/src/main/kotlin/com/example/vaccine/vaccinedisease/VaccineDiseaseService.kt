package com.example.vaccine.vaccinedisease

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
) {
    @Transactional(readOnly = true)
    fun listByVaccine(vaccineId: UUID): List<VaccineDiseaseEntity> {
        requireVaccineExists(vaccineId)
        return vaccineDiseaseRepository.findAllByIdVaccineId(vaccineId)
    }

    @Transactional
    fun createLink(
        vaccineId: UUID,
        diseaseId: Int,
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
    }

    @Transactional
    fun deleteLink(
        vaccineId: UUID,
        diseaseId: Int,
    ) {
        requireVaccineExists(vaccineId)
        requireDiseaseExists(diseaseId)
        val deleted = vaccineDiseaseRepository.deleteByIdVaccineIdAndIdDiseaseId(vaccineId, diseaseId)
        if (deleted == 0L) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccine-disease link not found")
        }
    }

    private fun requireVaccineExists(vaccineId: UUID) {
        if (!vaccineRepository.existsById(vaccineId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "vaccineId does not exist")
        }
    }

    private fun requireDiseaseExists(diseaseId: Int) {
        if (!diseaseRepository.existsById(diseaseId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "diseaseId does not exist")
        }
    }
}

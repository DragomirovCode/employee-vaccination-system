package com.example.vaccine.vaccine

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
) {
    @Transactional(readOnly = true)
    fun list(): List<VaccineEntity> = vaccineRepository.findAll()

    @Transactional(readOnly = true)
    fun get(id: UUID): VaccineEntity = findVaccine(id)

    @Transactional
    fun create(command: CreateVaccineCommand): VaccineEntity {
        requireUniqueName(command.name, null)
        return vaccineRepository.saveAndFlush(
            VaccineEntity(
                name = command.name.trim(),
                manufacturer = command.manufacturer?.trim(),
                validityDays = command.validityDays,
                dosesRequired = command.dosesRequired,
                daysBetween = command.daysBetween,
                isActive = command.isActive,
            ),
        )
    }

    @Transactional
    fun update(
        id: UUID,
        command: UpdateVaccineCommand,
    ): VaccineEntity {
        val vaccine = findVaccine(id)
        requireUniqueName(command.name, id)
        vaccine.name = command.name.trim()
        vaccine.manufacturer = command.manufacturer?.trim()
        vaccine.validityDays = command.validityDays
        vaccine.dosesRequired = command.dosesRequired
        vaccine.daysBetween = command.daysBetween
        vaccine.isActive = command.isActive
        return vaccineRepository.saveAndFlush(vaccine)
    }

    @Transactional
    fun delete(id: UUID) {
        findVaccine(id)
        if (vaccineDiseaseRepository.existsByIdVaccineId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Vaccine has disease links")
        }
        try {
            vaccineRepository.deleteById(id)
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

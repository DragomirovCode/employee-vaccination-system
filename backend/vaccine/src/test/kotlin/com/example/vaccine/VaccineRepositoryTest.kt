package com.example.vaccine

import com.example.vaccine.disease.DiseaseEntity
import com.example.vaccine.disease.DiseaseRepository
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import com.example.vaccine.vaccinedisease.VaccineDiseaseEntity
import com.example.vaccine.vaccinedisease.VaccineDiseaseId
import com.example.vaccine.vaccinedisease.VaccineDiseaseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(classes = [VaccineTestApplication::class])
class VaccineRepositoryTest {
    @Autowired
    private lateinit var diseaseRepository: DiseaseRepository

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var vaccineDiseaseRepository: VaccineDiseaseRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `create vaccine disease link`() {
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "Flu"))
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "Flu shot",
                    manufacturer = "Pfizer",
                    validityDays = 365,
                    dosesRequired = 1,
                    daysBetween = null,
                ),
            )

        vaccineDiseaseRepository.saveAndFlush(
            VaccineDiseaseEntity(
                id =
                    VaccineDiseaseId(
                        vaccineId = vaccine.id,
                        diseaseId = disease.id,
                    ),
            ),
        )

        val links = vaccineDiseaseRepository.findAllByIdVaccineId(vaccine.id!!)

        assertEquals(1, links.size)
        assertNotNull(vaccine.id)
        assertEquals(4, vaccine.id!!.version())
    }

    @Test
    fun `duplicate vaccine disease pair is rejected`() {
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "Hepatitis"))
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "HepaVac",
                    validityDays = 730,
                    dosesRequired = 3,
                    daysBetween = 30,
                ),
            )
        jdbcTemplate.update(
            "INSERT INTO vaccine_diseases (vaccine_id, disease_id) VALUES (?, ?)",
            vaccine.id,
            disease.id,
        )

        assertThrows<DataAccessException> {
            jdbcTemplate.update(
                "INSERT INTO vaccine_diseases (vaccine_id, disease_id) VALUES (?, ?)",
                vaccine.id,
                disease.id,
            )
        }
    }

    @Test
    fun `missing disease id fails by foreign key`() {
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "Unknown Disease Vaccine",
                    validityDays = 365,
                    dosesRequired = 1,
                ),
            )

        assertThrows<DataIntegrityViolationException> {
            vaccineDiseaseRepository.saveAndFlush(
                VaccineDiseaseEntity(
                    id =
                        VaccineDiseaseId(
                            vaccineId = vaccine.id,
                            diseaseId = Int.MAX_VALUE,
                        ),
                ),
            )
        }
    }

    @Test
    fun `missing vaccine id fails by foreign key`() {
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "Measles"))

        assertThrows<DataIntegrityViolationException> {
            vaccineDiseaseRepository.saveAndFlush(
                VaccineDiseaseEntity(
                    id =
                        VaccineDiseaseId(
                            vaccineId = UUID.randomUUID(),
                            diseaseId = disease.id,
                        ),
                ),
            )
        }
    }
}

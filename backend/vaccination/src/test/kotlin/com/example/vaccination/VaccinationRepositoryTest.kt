package com.example.vaccination

import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import com.example.vaccination.document.DocumentEntity
import com.example.vaccination.document.DocumentRepository
import com.example.vaccination.vaccination.VaccinationEntity
import com.example.vaccination.vaccination.VaccinationRepository
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [VaccinationTestApplication::class])
class VaccinationRepositoryTest {
    @Autowired
    private lateinit var vaccinationRepository: VaccinationRepository

    @Autowired
    private lateinit var documentRepository: DocumentRepository

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Test
    fun `save vaccination and document`() {
        documentRepository.deleteAll()
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()

        val user = userRepository.saveAndFlush(UserEntity(email = "doctor@example.com", passwordHash = "hash"))
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Medical"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Ivan",
                    lastName = "Doctorov",
                ),
            )
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "Flu shot",
                    validityDays = 365,
                    dosesRequired = 1,
                ),
            )

        val vaccination =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = employee.id,
                    vaccineId = vaccine.id,
                    performedBy = user.id,
                    vaccinationDate = LocalDate.of(2026, 3, 1),
                    doseNumber = 1,
                    revaccinationDate = LocalDate.of(2027, 3, 1),
                ),
            )

        val document =
            documentRepository.saveAndFlush(
                DocumentEntity(
                    vaccinationId = vaccination.id,
                    fileName = "certificate.pdf",
                    filePath = "vaccinations/certificate.pdf",
                    fileSize = 4096,
                    mimeType = "application/pdf",
                    uploadedBy = user.id,
                ),
            )

        assertNotNull(vaccination.id)
        assertEquals(7, vaccination.id!!.version())
        assertNotNull(document.id)
        assertEquals(7, document.id!!.version())
        assertEquals(1, vaccinationRepository.findAllByEmployeeId(employee.id!!).size)
        assertEquals(1, documentRepository.findAllByVaccinationId(vaccination.id!!).size)
    }
}

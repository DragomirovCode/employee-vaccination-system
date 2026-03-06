package com.example.vaccination

import com.example.audit.log.AuditAction
import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import com.example.vaccination.document.CreateDocumentCommand
import com.example.vaccination.document.DocumentService
import com.example.vaccination.document.UpdateDocumentCommand
import com.example.vaccination.vaccination.CreateVaccinationCommand
import com.example.vaccination.vaccination.UpdateVaccinationCommand
import com.example.vaccination.vaccination.VaccinationRepository
import com.example.vaccination.vaccination.VaccinationService
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [VaccinationTestApplication::class])
class AuditIntegrationTest {
    @Autowired
    private lateinit var vaccinationService: VaccinationService

    @Autowired
    private lateinit var documentService: DocumentService

    @Autowired
    private lateinit var vaccinationRepository: VaccinationRepository

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Test
    fun `writes audit records for vaccination create update delete`() {
        cleanup()
        val seed = seed()

        val created =
            vaccinationService.create(
                CreateVaccinationCommand(
                    employeeId = seed.employee.id!!,
                    vaccineId = seed.vaccine.id!!,
                    performedBy = seed.user.id!!,
                    vaccinationDate = LocalDate.of(2026, 3, 1),
                    doseNumber = 1,
                ),
            )
        vaccinationService.update(
            id = created.id!!,
            command =
                UpdateVaccinationCommand(
                    employeeId = seed.employee.id!!,
                    vaccineId = seed.vaccine.id!!,
                    performedBy = seed.user.id!!,
                    vaccinationDate = LocalDate.of(2026, 3, 2),
                    doseNumber = 1,
                    notes = "updated",
                ),
        )
        vaccinationService.delete(created.id!!, seed.user.id!!)

        val logs =
            auditLogRepository.findAllByEntityTypeAndEntityId(
                entityType = AuditEntityType.VACCINATION,
                entityId = created.id!!,
            )
        assertEquals(3, logs.size)
        assertTrue(logs.any { it.action == AuditAction.CREATE && it.newValue != null })
        assertTrue(logs.any { it.action == AuditAction.UPDATE && it.oldValue != null && it.newValue != null })
        assertTrue(logs.any { it.action == AuditAction.DELETE && it.oldValue != null && it.newValue == null })
    }

    @Test
    fun `writes audit records for document create update delete`() {
        cleanup()
        val seed = seed()

        val vaccination =
            vaccinationRepository.saveAndFlush(
                com.example.vaccination.vaccination.VaccinationEntity(
                    employeeId = seed.employee.id,
                    vaccineId = seed.vaccine.id,
                    performedBy = seed.user.id,
                    vaccinationDate = LocalDate.of(2026, 3, 1),
                    doseNumber = 1,
                    revaccinationDate = LocalDate.of(2027, 3, 1),
                ),
            )

        val created =
            documentService.create(
                CreateDocumentCommand(
                    vaccinationId = vaccination.id!!,
                    fileName = "a.pdf",
                    filePath = "docs/a.pdf",
                    fileSize = 100,
                    mimeType = "application/pdf",
                    uploadedBy = seed.user.id!!,
                ),
            )
        documentService.update(
            id = created.id!!,
            command =
                UpdateDocumentCommand(
                    vaccinationId = vaccination.id!!,
                    fileName = "b.pdf",
                    filePath = "docs/b.pdf",
                    fileSize = 200,
                    mimeType = "application/pdf",
                    modifiedBy = seed.user.id!!,
                ),
        )
        documentService.delete(created.id!!, seed.user.id!!)

        val logs =
            auditLogRepository.findAllByEntityTypeAndEntityId(
                entityType = AuditEntityType.DOCUMENT,
                entityId = created.id!!,
            )
        assertEquals(3, logs.size)
        assertTrue(logs.any { it.action == AuditAction.CREATE && it.newValue != null })
        assertTrue(logs.any { it.action == AuditAction.UPDATE && it.oldValue != null && it.newValue != null })
        assertTrue(logs.any { it.action == AuditAction.DELETE && it.oldValue != null && it.newValue == null })
    }

    private fun cleanup() {
        auditLogRepository.deleteAll()
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()
    }

    private fun seed(): Seed {
        val user = userRepository.saveAndFlush(UserEntity(email = "audit-vaccination@example.com", passwordHash = "hash"))
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Ops"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Alex",
                    lastName = "Worker",
                ),
            )
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "Two dose",
                    validityDays = 180,
                    dosesRequired = 2,
                    daysBetween = 30,
                ),
            )

        return Seed(user = user, employee = employee, vaccine = vaccine)
    }
}

private data class Seed(
    val user: UserEntity,
    val employee: EmployeeEntity,
    val vaccine: VaccineEntity,
)

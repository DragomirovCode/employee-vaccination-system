package com.example.vaccination.api

import com.example.audit.log.AuditLogRepository
import com.example.auth.role.RoleEntity
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleEntity
import com.example.auth.role.UserRoleId
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import com.example.vaccination.VaccinationTestApplication
import com.example.vaccination.document.DocumentEntity
import com.example.vaccination.document.DocumentRepository
import com.example.vaccination.vaccination.VaccinationEntity
import com.example.vaccination.vaccination.VaccinationRepository
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [VaccinationTestApplication::class])
class VaccinationWriteSecurityTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var vaccinationRepository: VaccinationRepository

    @Autowired
    private lateinit var documentRepository: DocumentRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `write requires auth`() {
        val seed = seedScopeData()

        mockMvc
            .perform(
                post("/vaccinations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccinationBody(seed.childEmployeeId, seed.vaccineId)),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `hr is forbidden for write`() {
        val seed = seedScopeData()
        val hr = createUserWithRole("HR")

        mockMvc
            .perform(
                post("/vaccinations")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccinationBody(seed.childEmployeeId, seed.vaccineId)),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `medical can write within own department tree`() {
        val seed = seedScopeData()

        mockMvc
            .perform(
                post("/vaccinations")
                    .header("X-Auth-Token", seed.medicalUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccinationBody(seed.childEmployeeId, seed.vaccineId)),
            ).andExpect(status().isOk)

        assertEquals(1, vaccinationRepository.count())
    }

    @Test
    fun `medical cannot create vaccination outside scope and no audit created`() {
        val seed = seedScopeData()
        val beforeAudit = auditLogRepository.count()
        val beforeVaccinations = vaccinationRepository.count()

        mockMvc
            .perform(
                post("/vaccinations")
                    .header("X-Auth-Token", seed.medicalUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccinationBody(seed.externalEmployeeId, seed.vaccineId)),
            ).andExpect(status().isForbidden)

        assertEquals(beforeVaccinations, vaccinationRepository.count())
        assertEquals(beforeAudit, auditLogRepository.count())
    }

    @Test
    fun `admin can create vaccination outside medical scope`() {
        val seed = seedScopeData()

        mockMvc
            .perform(
                post("/vaccinations")
                    .header("X-Auth-Token", seed.adminUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccinationBody(seed.externalEmployeeId, seed.vaccineId)),
            ).andExpect(status().isOk)

        assertEquals(1, vaccinationRepository.count())
    }

    @Test
    fun `medical cannot update vaccination outside scope and data unchanged`() {
        val seed = seedScopeData()
        val existing = createVaccination(seed.externalEmployeeId, seed.vaccineId, seed.adminUserId, notes = "before")
        val beforeAudit = auditLogRepository.count()

        mockMvc
            .perform(
                put("/vaccinations/${existing.id}")
                    .header("X-Auth-Token", seed.medicalUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccinationBody(seed.externalEmployeeId, seed.vaccineId, notes = "after")),
            ).andExpect(status().isForbidden)

        val unchanged = vaccinationRepository.findById(existing.id!!).orElseThrow()
        assertEquals("before", unchanged.notes)
        assertEquals(beforeAudit, auditLogRepository.count())
    }

    @Test
    fun `medical cannot delete vaccination outside scope`() {
        val seed = seedScopeData()
        val existing = createVaccination(seed.externalEmployeeId, seed.vaccineId, seed.adminUserId)
        val beforeAudit = auditLogRepository.count()

        mockMvc
            .perform(
                delete("/vaccinations/${existing.id}")
                    .header("X-Auth-Token", seed.medicalUserId.toString()),
            ).andExpect(status().isForbidden)

        assertEquals(true, vaccinationRepository.findById(existing.id!!).isPresent)
        assertEquals(beforeAudit, auditLogRepository.count())
    }

    @Test
    fun `medical cannot create document outside scope`() {
        val seed = seedScopeData()
        val externalVaccination = createVaccination(seed.externalEmployeeId, seed.vaccineId, seed.adminUserId)
        val beforeAudit = auditLogRepository.count()
        val beforeDocs = documentRepository.count()

        mockMvc
            .perform(
                post("/documents")
                    .header("X-Auth-Token", seed.medicalUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(documentBody(externalVaccination.id!!)),
            ).andExpect(status().isForbidden)

        assertEquals(beforeDocs, documentRepository.count())
        assertEquals(beforeAudit, auditLogRepository.count())
    }

    @Test
    fun `medical cannot update document outside scope`() {
        val seed = seedScopeData()
        val externalVaccination = createVaccination(seed.externalEmployeeId, seed.vaccineId, seed.adminUserId)
        val doc = createDocument(externalVaccination.id!!, seed.adminUserId, fileName = "before.pdf")
        val beforeAudit = auditLogRepository.count()

        mockMvc
            .perform(
                put("/documents/${doc.id}")
                    .header("X-Auth-Token", seed.medicalUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(documentBody(externalVaccination.id!!, fileName = "after.pdf")),
            ).andExpect(status().isForbidden)

        val unchanged = documentRepository.findById(doc.id!!).orElseThrow()
        assertEquals("before.pdf", unchanged.fileName)
        assertEquals(beforeAudit, auditLogRepository.count())
    }

    @Test
    fun `medical cannot delete document outside scope`() {
        val seed = seedScopeData()
        val externalVaccination = createVaccination(seed.externalEmployeeId, seed.vaccineId, seed.adminUserId)
        val doc = createDocument(externalVaccination.id!!, seed.adminUserId)
        val beforeAudit = auditLogRepository.count()

        mockMvc
            .perform(
                delete("/documents/${doc.id}")
                    .header("X-Auth-Token", seed.medicalUserId.toString()),
            ).andExpect(status().isForbidden)

        assertEquals(true, documentRepository.findById(doc.id!!).isPresent)
        assertEquals(beforeAudit, auditLogRepository.count())
    }

    private fun seedScopeData(): ScopeSeed {
        cleanup()

        val rootDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "MedicalRoot"))
        val childDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "MedicalChild", parentId = rootDepartment.id))
        val externalDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "External"))

        val medicalUser = createUserWithRole("MEDICAL")
        val adminUser = createUserWithRole("ADMIN")

        employeeRepository.saveAndFlush(
            EmployeeEntity(
                userId = medicalUser.id,
                departmentId = rootDepartment.id,
                firstName = "Med",
                lastName = "Worker",
            ),
        )

        val childEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = childDepartment.id,
                    firstName = "In",
                    lastName = "Scope",
                ),
            )

        val externalEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = externalDepartment.id,
                    firstName = "Out",
                    lastName = "Scope",
                ),
            )

        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "ScopeVax", validityDays = 365, dosesRequired = 1))

        return ScopeSeed(
            medicalUserId = medicalUser.id!!,
            adminUserId = adminUser.id!!,
            childEmployeeId = childEmployee.id!!,
            externalEmployeeId = externalEmployee.id!!,
            vaccineId = vaccine.id!!,
        )
    }

    private fun cleanup() {
        auditLogRepository.deleteAll()
        documentRepository.deleteAll()
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()
    }

    private fun createUserWithRole(roleCode: String): UserEntity {
        val user = userRepository.saveAndFlush(UserEntity(email = "$roleCode-${UUID.randomUUID()}@example.com", passwordHash = "hash"))
        val role = ensureRole(roleCode)
        userRoleRepository.saveAndFlush(UserRoleEntity(id = UserRoleId(userId = user.id, roleId = role.id)))
        return user
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))

    private fun createVaccination(
        employeeId: UUID,
        vaccineId: UUID,
        performerId: UUID,
        notes: String = "created",
    ): VaccinationEntity {
        val created =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = employeeId,
                    vaccineId = vaccineId,
                    performedBy = performerId,
                    vaccinationDate = LocalDate.of(2026, 3, 1),
                    doseNumber = 1,
                    revaccinationDate = LocalDate.of(2027, 3, 1),
                    notes = notes,
                ),
            )
        assertNotNull(created.id)
        return created
    }

    private fun createDocument(
        vaccinationId: UUID,
        uploaderId: UUID,
        fileName: String = "doc.pdf",
    ): DocumentEntity {
        val created =
            documentRepository.saveAndFlush(
                DocumentEntity(
                    vaccinationId = vaccinationId,
                    fileName = fileName,
                    filePath = "docs/$fileName",
                    fileSize = 123,
                    mimeType = "application/pdf",
                    uploadedBy = uploaderId,
                ),
            )
        assertNotNull(created.id)
        return created
    }

    private fun vaccinationBody(
        employeeId: UUID,
        vaccineId: UUID,
        notes: String = "created",
    ): String =
        """
        {
          "employeeId": "$employeeId",
          "vaccineId": "$vaccineId",
          "vaccinationDate": "2026-03-01",
          "doseNumber": 1,
          "batchNumber": "B-1",
          "expirationDate": "2027-01-01",
          "notes": "$notes"
        }
        """.trimIndent()

    private fun documentBody(
        vaccinationId: UUID,
        fileName: String = "doc.pdf",
    ): String =
        """
        {
          "vaccinationId": "$vaccinationId",
          "fileName": "$fileName",
          "filePath": "docs/$fileName",
          "fileSize": 123,
          "mimeType": "application/pdf"
        }
        """.trimIndent()
}

private data class ScopeSeed(
    val medicalUserId: UUID,
    val adminUserId: UUID,
    val childEmployeeId: UUID,
    val externalEmployeeId: UUID,
    val vaccineId: UUID,
)

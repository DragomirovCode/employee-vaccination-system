package com.example.vaccination.api

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
    fun `vaccination create requires auth`() {
        val seed = seedBase()

        mockMvc
            .perform(
                post("/vaccinations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        vaccinationBody(
                            employeeId = seed.employee.id!!,
                            vaccineId = seed.vaccine.id!!,
                        ),
                    ),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `vaccination create forbidden for HR and does not change data`() {
        val seed = seedBase()
        val hr = createUserWithRole("HR")
        val before = vaccinationRepository.count()

        mockMvc
            .perform(
                post("/vaccinations")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        vaccinationBody(
                            employeeId = seed.employee.id!!,
                            vaccineId = seed.vaccine.id!!,
                        ),
                    ),
            ).andExpect(status().isForbidden)

        assertEquals(before, vaccinationRepository.count())
    }

    @Test
    fun `vaccination create success for MEDICAL`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")

        mockMvc
            .perform(
                post("/vaccinations")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        vaccinationBody(
                            employeeId = seed.employee.id!!,
                            vaccineId = seed.vaccine.id!!,
                        ),
                    ),
            ).andExpect(status().isOk)

        assertEquals(1, vaccinationRepository.count())
    }

    @Test
    fun `vaccination update forbidden for PERSON and keeps original data`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val person = createUserWithRole("PERSON")
        val created = createVaccination(seed, medical.id!!)

        mockMvc
            .perform(
                put("/vaccinations/${created.id}")
                    .header("X-Auth-Token", person.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        vaccinationBody(
                            employeeId = seed.employee.id!!,
                            vaccineId = seed.vaccine.id!!,
                            notes = "forbidden-update",
                        ),
                    ),
            ).andExpect(status().isForbidden)

        val unchanged = vaccinationRepository.findById(created.id!!).orElseThrow()
        assertEquals(created.notes, unchanged.notes)
    }

    @Test
    fun `vaccination update success for ADMIN`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val admin = createUserWithRole("ADMIN")
        val created = createVaccination(seed, medical.id!!)

        mockMvc
            .perform(
                put("/vaccinations/${created.id}")
                    .header("X-Auth-Token", admin.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        vaccinationBody(
                            employeeId = seed.employee.id!!,
                            vaccineId = seed.vaccine.id!!,
                            notes = "updated-by-admin",
                        ),
                    ),
            ).andExpect(status().isOk)

        val updated = vaccinationRepository.findById(created.id!!).orElseThrow()
        assertEquals("updated-by-admin", updated.notes)
    }

    @Test
    fun `vaccination delete forbidden for HR and keeps record`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val hr = createUserWithRole("HR")
        val created = createVaccination(seed, medical.id!!)

        mockMvc
            .perform(
                delete("/vaccinations/${created.id}")
                    .header("X-Auth-Token", hr.id.toString()),
            ).andExpect(status().isForbidden)

        assertEquals(true, vaccinationRepository.findById(created.id!!).isPresent)
    }

    @Test
    fun `vaccination delete success for MEDICAL`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val created = createVaccination(seed, medical.id!!)

        mockMvc
            .perform(
                delete("/vaccinations/${created.id}")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isNoContent)

        assertEquals(false, vaccinationRepository.findById(created.id!!).isPresent)
    }

    @Test
    fun `document create requires auth`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val vaccination = createVaccination(seed, medical.id!!)

        mockMvc
            .perform(
                post("/documents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(documentBody(vaccination.id!!)),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `document create forbidden for PERSON and does not change data`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val person = createUserWithRole("PERSON")
        val vaccination = createVaccination(seed, medical.id!!)
        val before = documentRepository.count()

        mockMvc
            .perform(
                post("/documents")
                    .header("X-Auth-Token", person.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(documentBody(vaccination.id!!)),
            ).andExpect(status().isForbidden)

        assertEquals(before, documentRepository.count())
    }

    @Test
    fun `document create success for MEDICAL`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val vaccination = createVaccination(seed, medical.id!!)

        mockMvc
            .perform(
                post("/documents")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(documentBody(vaccination.id!!)),
            ).andExpect(status().isOk)

        assertEquals(1, documentRepository.count())
    }

    @Test
    fun `document update forbidden for HR and keeps original file name`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val hr = createUserWithRole("HR")
        val vaccination = createVaccination(seed, medical.id!!)
        val doc = createDocument(vaccination.id!!, medical.id!!)

        mockMvc
            .perform(
                put("/documents/${doc.id}")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(documentBody(vaccination.id!!, fileName = "forbidden.pdf")),
            ).andExpect(status().isForbidden)

        val unchanged = documentRepository.findById(doc.id!!).orElseThrow()
        assertEquals(doc.fileName, unchanged.fileName)
    }

    @Test
    fun `document update success for ADMIN`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val admin = createUserWithRole("ADMIN")
        val vaccination = createVaccination(seed, medical.id!!)
        val doc = createDocument(vaccination.id!!, medical.id!!)

        mockMvc
            .perform(
                put("/documents/${doc.id}")
                    .header("X-Auth-Token", admin.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(documentBody(vaccination.id!!, fileName = "updated.pdf")),
            ).andExpect(status().isOk)

        val updated = documentRepository.findById(doc.id!!).orElseThrow()
        assertEquals("updated.pdf", updated.fileName)
    }

    @Test
    fun `document delete forbidden for PERSON and keeps record`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val person = createUserWithRole("PERSON")
        val vaccination = createVaccination(seed, medical.id!!)
        val doc = createDocument(vaccination.id!!, medical.id!!)

        mockMvc
            .perform(
                delete("/documents/${doc.id}")
                    .header("X-Auth-Token", person.id.toString()),
            ).andExpect(status().isForbidden)

        assertEquals(true, documentRepository.findById(doc.id!!).isPresent)
    }

    @Test
    fun `document delete success for MEDICAL`() {
        val seed = seedBase()
        val medical = createUserWithRole("MEDICAL")
        val vaccination = createVaccination(seed, medical.id!!)
        val doc = createDocument(vaccination.id!!, medical.id!!)

        mockMvc
            .perform(
                delete("/documents/${doc.id}")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isNoContent)

        assertEquals(false, documentRepository.findById(doc.id!!).isPresent)
    }

    private fun seedBase(): BaseSeed {
        cleanup()

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
                    name = "CoreVaccine",
                    validityDays = 365,
                    dosesRequired = 1,
                ),
            )

        return BaseSeed(employee = employee, vaccine = vaccine)
    }

    private fun cleanup() {
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
        userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = UserRoleId(userId = user.id, roleId = role.id),
            ),
        )
        return user
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))

    private fun createVaccination(seed: BaseSeed, performerId: UUID): VaccinationEntity {
        val created =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = seed.employee.id,
                    vaccineId = seed.vaccine.id,
                    performedBy = performerId,
                    vaccinationDate = LocalDate.of(2026, 3, 1),
                    doseNumber = 1,
                    revaccinationDate = LocalDate.of(2027, 3, 1),
                    notes = "initial",
                ),
            )
        assertNotNull(created.id)
        return created
    }

    private fun createDocument(vaccinationId: UUID, uploaderId: UUID): DocumentEntity {
        val created =
            documentRepository.saveAndFlush(
                DocumentEntity(
                    vaccinationId = vaccinationId,
                    fileName = "initial.pdf",
                    filePath = "docs/initial.pdf",
                    fileSize = 100,
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

private data class BaseSeed(
    val employee: EmployeeEntity,
    val vaccine: VaccineEntity,
)

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [VaccinationTestApplication::class])
class DocumentContentApiTest {
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
        cleanup()
    }

    @Test
    fun `admin uploads and person downloads own document content`() {
        val seed = seedData()
        val file = MockMultipartFile("file", "cert.pdf", "application/pdf", "hello-pdf".toByteArray())

        mockMvc
            .perform(
                multipart("/documents/${seed.ownDocumentId}/content")
                    .file(file)
                    .header("X-Auth-Token", seed.adminUserId.toString()),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                get("/documents/${seed.ownDocumentId}/content")
                    .header("X-Auth-Token", seed.personUserId.toString()),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"cert.pdf\""))
            .andExpect(content().bytes("hello-pdf".toByteArray()))
    }

    @Test
    fun `person cannot upload document content`() {
        val seed = seedData()
        val file = MockMultipartFile("file", "x.pdf", "application/pdf", "x".toByteArray())

        mockMvc
            .perform(
                multipart("/documents/${seed.ownDocumentId}/content")
                    .file(file)
                    .header("X-Auth-Token", seed.personUserId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `person cannot download external document content`() {
        val seed = seedData()
        val file = MockMultipartFile("file", "ext.pdf", "application/pdf", "external".toByteArray())

        mockMvc
            .perform(
                multipart("/documents/${seed.externalDocumentId}/content")
                    .file(file)
                    .header("X-Auth-Token", seed.adminUserId.toString()),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                get("/documents/${seed.externalDocumentId}/content")
                    .header("X-Auth-Token", seed.personUserId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `delete content removes file from storage`() {
        val seed = seedData()
        val file = MockMultipartFile("file", "to-delete.pdf", "application/pdf", "del".toByteArray())

        mockMvc
            .perform(
                multipart("/documents/${seed.ownDocumentId}/content")
                    .file(file)
                    .header("X-Auth-Token", seed.adminUserId.toString()),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                delete("/documents/${seed.ownDocumentId}/content")
                    .header("X-Auth-Token", seed.adminUserId.toString()),
            ).andExpect(status().isNoContent)

        mockMvc
            .perform(
                get("/documents/${seed.ownDocumentId}/content")
                    .header("X-Auth-Token", seed.adminUserId.toString()),
            ).andExpect(status().isNotFound)
    }

    private fun seedData(): DocumentContentSeedData {
        val personUser = createUserWithRole("PERSON")
        val adminUser = createUserWithRole("ADMIN")
        val performer = adminUser

        val ownDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "Own"))
        val externalDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "External"))

        val ownEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    userId = personUser.id,
                    departmentId = ownDepartment.id,
                    firstName = "Own",
                    lastName = "User",
                ),
            )

        val externalEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = externalDepartment.id,
                    firstName = "External",
                    lastName = "User",
                ),
            )

        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "ContentVax", validityDays = 365, dosesRequired = 1))

        val ownVaccination =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = ownEmployee.id,
                    vaccineId = vaccine.id,
                    performedBy = performer.id,
                    vaccinationDate = LocalDate.of(2026, 3, 1),
                    doseNumber = 1,
                    expirationDate = LocalDate.of(2027, 1, 1),
                    revaccinationDate = LocalDate.of(2027, 3, 1),
                ),
            )

        val externalVaccination =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = externalEmployee.id,
                    vaccineId = vaccine.id,
                    performedBy = performer.id,
                    vaccinationDate = LocalDate.of(2026, 4, 1),
                    doseNumber = 1,
                    expirationDate = LocalDate.of(2027, 2, 1),
                    revaccinationDate = LocalDate.of(2027, 4, 1),
                ),
            )

        val ownDocument =
            documentRepository.saveAndFlush(
                DocumentEntity(
                    vaccinationId = ownVaccination.id,
                    fileName = "own.pdf",
                    filePath = "documents/${UUID.randomUUID()}/own.pdf",
                    fileSize = 1,
                    mimeType = "application/pdf",
                    uploadedBy = performer.id,
                ),
            )

        val externalDocument =
            documentRepository.saveAndFlush(
                DocumentEntity(
                    vaccinationId = externalVaccination.id,
                    fileName = "external.pdf",
                    filePath = "documents/${UUID.randomUUID()}/external.pdf",
                    fileSize = 1,
                    mimeType = "application/pdf",
                    uploadedBy = performer.id,
                ),
            )

        return DocumentContentSeedData(
            personUserId = personUser.id!!,
            adminUserId = adminUser.id!!,
            ownDocumentId = ownDocument.id!!,
            externalDocumentId = externalDocument.id!!,
        )
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
}

private data class DocumentContentSeedData(
    val personUserId: UUID,
    val adminUserId: UUID,
    val ownDocumentId: UUID,
    val externalDocumentId: UUID,
)

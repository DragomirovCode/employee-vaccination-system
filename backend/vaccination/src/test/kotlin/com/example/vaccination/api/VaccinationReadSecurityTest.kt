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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [VaccinationTestApplication::class])
class VaccinationReadSecurityTest {
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
    fun `read endpoints require auth`() {
        mockMvc
            .perform(get("/vaccinations"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `person can read only own vaccinations`() {
        val seed = seedData()

        mockMvc
            .perform(
                get("/employees/${seed.personEmployeeId}/vaccinations")
                    .header("X-Auth-Token", seed.personUserId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc
            .perform(
                get("/employees/${seed.externalEmployeeId}/vaccinations")
                    .header("X-Auth-Token", seed.personUserId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `hr cannot request employee outside department scope`() {
        val seed = seedData()

        mockMvc
            .perform(
                get("/vaccinations")
                    .header("X-Auth-Token", seed.hrUserId.toString())
                    .param("employeeId", seed.externalEmployeeId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `medical can read any document`() {
        val seed = seedData()

        mockMvc
            .perform(
                get("/documents/${seed.externalDocumentId}")
                    .header("X-Auth-Token", seed.medicalUserId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(seed.externalDocumentId.toString()))
    }

    @Test
    fun `invalid date range returns bad request`() {
        val seed = seedData()

        mockMvc
            .perform(
                get("/vaccinations")
                    .header("X-Auth-Token", seed.hrUserId.toString())
                    .param("dateFrom", "2026-12-31")
                    .param("dateTo", "2026-01-01"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    private fun seedData(): VaccinationReadSeedData {
        val personUser = createUserWithRole("PERSON")
        val hrUser = createUserWithRole("HR")
        val medicalUser = createUserWithRole("MEDICAL")
        val performer = createUserWithRole("ADMIN")

        val rootDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "Root"))
        val childDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "Child", parentId = rootDepartment.id))
        val externalDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "External"))

        val personEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    userId = personUser.id,
                    departmentId = childDepartment.id,
                    firstName = "Person",
                    lastName = "User",
                ),
            )

        employeeRepository.saveAndFlush(
            EmployeeEntity(
                userId = hrUser.id,
                departmentId = rootDepartment.id,
                firstName = "Hr",
                lastName = "User",
            ),
        )

        employeeRepository.saveAndFlush(
            EmployeeEntity(
                userId = medicalUser.id,
                departmentId = rootDepartment.id,
                firstName = "Medical",
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

        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "ReadVax", validityDays = 365, dosesRequired = 1))

        val ownVaccination =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = personEmployee.id,
                    vaccineId = vaccine.id,
                    performedBy = performer.id,
                    vaccinationDate = LocalDate.of(2026, 1, 10),
                    doseNumber = 1,
                    expirationDate = LocalDate.of(2027, 1, 1),
                    revaccinationDate = LocalDate.of(2027, 1, 10),
                ),
            )

        val externalVaccination =
            vaccinationRepository.saveAndFlush(
                VaccinationEntity(
                    employeeId = externalEmployee.id,
                    vaccineId = vaccine.id,
                    performedBy = performer.id,
                    vaccinationDate = LocalDate.of(2026, 2, 10),
                    doseNumber = 1,
                    expirationDate = LocalDate.of(2027, 2, 1),
                    revaccinationDate = LocalDate.of(2027, 2, 10),
                ),
            )

        documentRepository.saveAndFlush(
            DocumentEntity(
                vaccinationId = ownVaccination.id,
                fileName = "own.pdf",
                filePath = "docs/own.pdf",
                fileSize = 100,
                mimeType = "application/pdf",
                uploadedBy = performer.id,
            ),
        )

        val externalDocument =
            documentRepository.saveAndFlush(
                DocumentEntity(
                    vaccinationId = externalVaccination.id,
                    fileName = "external.pdf",
                    filePath = "docs/external.pdf",
                    fileSize = 100,
                    mimeType = "application/pdf",
                    uploadedBy = performer.id,
                ),
            )

        return VaccinationReadSeedData(
            personUserId = personUser.id!!,
            hrUserId = hrUser.id!!,
            medicalUserId = medicalUser.id!!,
            personEmployeeId = personEmployee.id!!,
            externalEmployeeId = externalEmployee.id!!,
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

private data class VaccinationReadSeedData(
    val personUserId: UUID,
    val hrUserId: UUID,
    val medicalUserId: UUID,
    val personEmployeeId: UUID,
    val externalEmployeeId: UUID,
    val externalDocumentId: UUID,
)

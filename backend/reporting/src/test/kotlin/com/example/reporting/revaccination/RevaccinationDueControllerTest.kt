package com.example.reporting.revaccination

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
import com.example.reporting.ReportingTestApplication
import com.example.vaccination.vaccination.VaccinationEntity
import com.example.vaccination.vaccination.VaccinationRepository
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [ReportingTestApplication::class])
class RevaccinationDueControllerTest {
    private lateinit var mockMvc: MockMvc

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

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `person sees only own records`() {
        val seeded = seedData("PERSON")

        mockMvc
            .perform(
                get("/reports/revaccination-due")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].employeeId").value(seeded.authEmployeeId.toString()))
    }

    @Test
    fun `hr sees only own department tree`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `hr gets forbidden for department outside scope`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30")
                    .param("departmentId", seeded.externalDepartmentId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `returns unauthorized when auth token missing`() {
        seedData("HR")

        mockMvc
            .perform(get("/reports/revaccination-due").param("days", "10"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `returns bad request for negative days`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "-1"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `exports revaccination due report as csv`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due/export")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"revaccination-due.csv\""))
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Employee,Department,Vaccine,Last vaccination date,Revaccination date,Days left")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("User Auth,Root,Influenza")))
    }

    @Test
    fun `exports revaccination due report as csv in russian`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due/export")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .header("Accept-Language", "ru")
                    .param("days", "30"),
            ).andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Сотрудник,Подразделение,Вакцина,Дата последней вакцинации,Дата ревакцинации,Осталось дней")))
    }

    @Test
    fun `exports revaccination due report as xlsx`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due/export")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30")
                    .param("format", "xlsx"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"revaccination-due.xlsx\""))
            .andExpect(
                header().string(
                    "Content-Type",
                    org.hamcrest.Matchers.containsString(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    ),
                ),
            ).andExpect {
                val bytes = it.response.contentAsByteArray
                assertTrue(
                    bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte(),
                    "Expected XLSX zip signature (PK)",
                )
            }
    }

    @Test
    fun `exports revaccination due report as pdf`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due/export")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30")
                    .param("format", "pdf"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"revaccination-due.pdf\""))
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
            .andExpect {
                val bytes = it.response.contentAsByteArray
                assertTrue(
                    bytes.size >= 4 &&
                        bytes[0] == '%'.code.toByte() &&
                        bytes[1] == 'P'.code.toByte() &&
                        bytes[2] == 'D'.code.toByte() &&
                        bytes[3] == 'F'.code.toByte(),
                    "Expected PDF signature (%PDF)",
                )
            }
    }

    @Test
    fun `export returns bad request for unsupported format`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due/export")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30")
                    .param("format", "xml"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `export returns forbidden for hr department outside scope`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due/export")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "30")
                    .param("departmentId", seeded.externalDepartmentId.toString()),
            ).andExpect(status().isForbidden)
    }

    private fun seedData(roleCode: String): SeededRecord {
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()

        val authUser = userRepository.saveAndFlush(UserEntity(email = "reporting-$roleCode@example.com", passwordHash = "hash"))
        val performer = userRepository.saveAndFlush(UserEntity(email = "medic-api@example.com", passwordHash = "hash"))
        val role = ensureRole(roleCode)
        userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = UserRoleId(userId = authUser.id, roleId = role.id),
                assignedBy = performer.id,
            ),
        )

        val rootDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "Root"))
        val childDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "Child", parentId = rootDepartment.id))
        val externalDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "External"))

        val authEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    userId = authUser.id,
                    departmentId = rootDepartment.id,
                    firstName = "Auth",
                    lastName = "User",
                ),
            )

        val childEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = childDepartment.id,
                    firstName = "Child",
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

        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "Influenza", validityDays = 365, dosesRequired = 1))
        val today = LocalDate.now()

        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = authEmployee.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = today.minusDays(200),
                doseNumber = 1,
                revaccinationDate = today.plusDays(3),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = childEmployee.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = today.minusDays(170),
                doseNumber = 1,
                revaccinationDate = today.plusDays(5),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = externalEmployee.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = today.minusDays(150),
                doseNumber = 1,
                revaccinationDate = today.plusDays(7),
            ),
        )

        return SeededRecord(
            authUserId = authUser.id!!,
            authEmployeeId = authEmployee.id!!,
            externalDepartmentId = externalDepartment.id!!,
        )
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))
}

private data class SeededRecord(
    val authUserId: UUID,
    val authEmployeeId: UUID,
    val externalDepartmentId: UUID,
)

package com.example.reporting.coverage

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
class VaccinationCoverageControllerTest {
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
    fun `person sees only own coverage`() {
        val seed = seedData("PERSON")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].employeesTotal").value(1))
            .andExpect(jsonPath("$[0].employeesCovered").value(1))
    }

    @Test
    fun `hr gets forbidden for department outside scope`() {
        val seed = seedData("HR")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("departmentId", seed.externalDepartmentId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `medical sees full coverage`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun `returns unauthorized when auth token missing`() {
        seedData("HR")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage")
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31"),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `exports vaccination coverage as csv`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"vaccination-coverage.csv\""))
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
            .andExpect(
                content().string(org.hamcrest.Matchers.containsString("Department,Employees total,Employees covered,Coverage percent")),
            ).andExpect(content().string(org.hamcrest.Matchers.containsString("Root,1,1,100.0")))
    }

    @Test
    fun `exports vaccination coverage as xlsx`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("format", "xlsx"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"vaccination-coverage.xlsx\""))
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
    fun `exports vaccination coverage as pdf`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("format", "pdf"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"vaccination-coverage.pdf\""))
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
    fun `coverage export returns bad request for unsupported format`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("format", "xml"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `coverage export returns forbidden for department outside scope`() {
        val seed = seedData("HR")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("departmentId", seed.externalDepartmentId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `medical sees vaccine coverage report`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].vaccineName").exists())
            .andExpect(jsonPath("$[0].employeesTotal").value(3))
    }

    @Test
    fun `vaccine coverage returns forbidden for department outside scope`() {
        val seed = seedData("HR")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("departmentId", seed.externalDepartmentId.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `vaccine coverage returns bad request for invalid date range`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-12-31")
                    .param("dateTo", "2026-01-01"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `exports vaccine coverage as csv`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"vaccination-coverage-by-vaccine.csv\""))
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Vaccine,Employees total,Employees covered,Coverage percent")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Coverage API Vax-A,3,3,100.0")))
    }

    @Test
    fun `exports vaccine coverage as xlsx`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("format", "xlsx"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"vaccination-coverage-by-vaccine.xlsx\""))
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
    fun `exports vaccine coverage as pdf`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("format", "pdf"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"vaccination-coverage-by-vaccine.pdf\""))
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
    fun `vaccine coverage export returns bad request for unsupported format`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("format", "xml"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `vaccine coverage export returns forbidden for department outside scope`() {
        val seed = seedData("HR")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage-by-vaccine/export")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("departmentId", seed.externalDepartmentId.toString()),
            ).andExpect(status().isForbidden)
    }

    private fun seedData(roleCode: String): SeededData {
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()

        val authUser = userRepository.saveAndFlush(UserEntity(email = "coverage-$roleCode@example.com", passwordHash = "hash"))
        val performer = userRepository.saveAndFlush(UserEntity(email = "coverage-api@example.com", passwordHash = "hash"))
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
                    lastName = "Coverage",
                ),
            )

        val childEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = childDepartment.id,
                    firstName = "Child",
                    lastName = "Coverage",
                ),
            )

        val externalEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = externalDepartment.id,
                    firstName = "External",
                    lastName = "Coverage",
                ),
            )

        val vaccineA = vaccineRepository.saveAndFlush(VaccineEntity(name = "Coverage API Vax-A", validityDays = 365, dosesRequired = 1))
        val vaccineB = vaccineRepository.saveAndFlush(VaccineEntity(name = "Coverage API Vax-B", validityDays = 365, dosesRequired = 1))

        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = authEmployee.id,
                vaccineId = vaccineA.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 3, 1),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(60),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = childEmployee.id,
                vaccineId = vaccineA.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 4, 1),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(30),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = childEmployee.id,
                vaccineId = vaccineB.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 6, 1),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(30),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = externalEmployee.id,
                vaccineId = vaccineA.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 5, 1),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(90),
            ),
        )

        return SeededData(
            authUserId = authUser.id!!,
            externalDepartmentId = externalDepartment.id!!,
        )
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))
}

private data class SeededData(
    val authUserId: UUID,
    val externalDepartmentId: UUID,
)

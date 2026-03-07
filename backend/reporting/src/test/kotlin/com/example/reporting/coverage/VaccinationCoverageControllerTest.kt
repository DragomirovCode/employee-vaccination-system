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
    fun `returns vaccination coverage report for MEDICAL role`() {
        val seed = seedData("MEDICAL")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31")
                    .param("departmentId", seed.departmentId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].departmentId").value(seed.departmentId.toString()))
            .andExpect(jsonPath("$[0].employeesTotal").value(2))
            .andExpect(jsonPath("$[0].employeesCovered").value(1))
            .andExpect(jsonPath("$[0].coveragePercent").value(50.0))
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
    fun `returns forbidden for PERSON role`() {
        val seed = seedData("PERSON")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-01-01")
                    .param("dateTo", "2026-12-31"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `returns bad request when dateFrom is after dateTo`() {
        val seed = seedData("HR")

        mockMvc
            .perform(
                get("/reports/vaccination-coverage")
                    .header("X-Auth-Token", seed.authUserId.toString())
                    .param("dateFrom", "2026-12-31")
                    .param("dateTo", "2026-01-01"),
            ).andExpect(status().isBadRequest)
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

        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Coverage Controller Department"))
        val employeeCovered =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Covered",
                    lastName = "Employee",
                ),
            )
        val employeeNotCovered =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "NotCovered",
                    lastName = "Employee",
                ),
            )

        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "Coverage API Vax", validityDays = 365, dosesRequired = 1))

        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = employeeCovered.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 3, 1),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(60),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = employeeNotCovered.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 4, 1),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().minusDays(1),
            ),
        )

        return SeededData(
            authUserId = authUser.id!!,
            departmentId = department.id!!,
        )
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))
}

private data class SeededData(
    val authUserId: UUID,
    val departmentId: UUID,
)

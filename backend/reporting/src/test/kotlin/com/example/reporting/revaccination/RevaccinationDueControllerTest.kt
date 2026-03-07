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
    fun `returns paged report with department filter for HR role`() {
        val seeded = seedData("HR")

        mockMvc
            .perform(
                get("/reports/revaccination-due")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "10")
                    .param("departmentId", seeded.departmentId.toString())
                    .param("page", "0")
                    .param("size", "10"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].employeeId").value(seeded.employeeId.toString()))
            .andExpect(jsonPath("$.content[0].vaccineName").value("Influenza"))
    }

    @Test
    fun `returns unauthorized when auth token missing`() {
        seedData("HR")

        mockMvc
            .perform(get("/reports/revaccination-due").param("days", "10"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `returns forbidden for PERSON role`() {
        val seeded = seedData("PERSON")

        mockMvc
            .perform(
                get("/reports/revaccination-due")
                    .header("X-Auth-Token", seeded.authUserId.toString())
                    .param("days", "10"),
            ).andExpect(status().isForbidden)
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

        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Factory"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Petr",
                    lastName = "Ivanov",
                ),
            )
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "Influenza", validityDays = 365, dosesRequired = 1))

        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = employee.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.now().minusDays(200),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(3),
            ),
        )

        return SeededRecord(
            authUserId = authUser.id!!,
            departmentId = department.id!!,
            employeeId = employee.id!!,
        )
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))
}

private data class SeededRecord(
    val authUserId: UUID,
    val departmentId: UUID,
    val employeeId: UUID,
)

package com.example.employee.api

import com.example.auth.role.RoleEntity
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleEntity
import com.example.auth.role.UserRoleId
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.EmployeeTestApplication
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest(classes = [EmployeeTestApplication::class])
class EmployeeApiTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        cleanup()
    }

    @Test
    fun `write is forbidden for person and allowed for hr`() {
        val person = createUserWithRole("PERSON")
        val hr = createUserWithRole("HR")

        mockMvc
            .perform(
                post("/departments")
                    .header("X-Auth-Token", person.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"HR Ops"}"""),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(
                post("/departments")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"HR Ops"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("HR Ops"))
    }

    @Test
    fun `read requires authentication`() {
        mockMvc
            .perform(get("/departments"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `department cycle is rejected`() {
        val hr = createUserWithRole("HR")
        val root = departmentRepository.saveAndFlush(DepartmentEntity(name = "Root"))
        val child = departmentRepository.saveAndFlush(DepartmentEntity(name = "Child", parentId = root.id))

        mockMvc
            .perform(
                put("/departments/${root.id}")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Root","parentId":"${child.id}"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `employee create with unknown department returns bad request`() {
        val hr = createUserWithRole("HR")
        val unknownDepartment = UUID.randomUUID()

        mockMvc
            .perform(
                post("/employees")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "departmentId":"$unknownDepartment",
                          "firstName":"Ivan",
                          "lastName":"Petrov"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `employee duplicate user id returns conflict`() {
        val hr = createUserWithRole("HR")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Finance"))
        val linkedUserId = UUID.randomUUID()

        mockMvc
            .perform(
                post("/employees")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "userId":"$linkedUserId",
                          "departmentId":"${department.id}",
                          "firstName":"Anna",
                          "lastName":"Sidorova"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                post("/employees")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "userId":"$linkedUserId",
                          "departmentId":"${department.id}",
                          "firstName":"Petr",
                          "lastName":"Sidorov"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    private fun createUserWithRole(roleCode: String): UserEntity {
        val user =
            userRepository.saveAndFlush(
                UserEntity(
                    email = "$roleCode-${UUID.randomUUID()}@example.com",
                    passwordHash = "hash",
                ),
            )
        val role = ensureRole(roleCode)
        userRoleRepository.saveAndFlush(UserRoleEntity(id = UserRoleId(userId = user.id, roleId = role.id)))
        return user
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))

    private fun cleanup() {
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
    }
}

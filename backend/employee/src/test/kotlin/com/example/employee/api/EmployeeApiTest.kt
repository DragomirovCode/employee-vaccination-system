package com.example.employee.api

import com.example.audit.log.AuditAction
import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

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

    @Test
    fun `employee delete is forbidden for hr`() {
        val hr = createUserWithRole("HR")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Operations"))
        val employeeId =
            UUID.fromString(
                extractJsonField(
                    mockMvc
                        .perform(
                            post("/employees")
                                .header("X-Auth-Token", hr.id.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                    """
                                    {
                                      "departmentId":"${department.id}",
                                      "firstName":"Delete",
                                      "lastName":"Target"
                                    }
                                    """.trimIndent(),
                                ),
                        ).andExpect(status().isCreated)
                        .andReturn()
                        .response
                        .contentAsString,
                    "id",
                ),
            )

        mockMvc
            .perform(
                delete("/employees/$employeeId")
                    .header("X-Auth-Token", hr.id.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `employee delete returns conflict when employee has linked user account`() {
        val admin = createUserWithRole("ADMIN")
        val linkedUser = createUserWithRole("PERSON")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Operations"))
        val employee =
            employeeRepository.saveAndFlush(
                com.example.employee.person.EmployeeEntity(
                    userId = linkedUser.id,
                    departmentId = department.id,
                    firstName = "Linked",
                    lastName = "Employee",
                ),
            )

        mockMvc
            .perform(
                delete("/employees/${employee.id}")
                    .header("X-Auth-Token", admin.id.toString()),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Employee has linked user account"))
    }

    @Test
    fun `admin can delete employee without linked user account`() {
        val admin = createUserWithRole("ADMIN")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Operations"))
        val employee =
            employeeRepository.saveAndFlush(
                com.example.employee.person.EmployeeEntity(
                    departmentId = department.id,
                    firstName = "No",
                    lastName = "Account",
                ),
            )

        mockMvc
            .perform(
                delete("/employees/${employee.id}")
                    .header("X-Auth-Token", admin.id.toString()),
            ).andExpect(status().isNoContent)
    }

    @Test
    fun `hr sees only employees from own department tree`() {
        val hrUser = createUserWithRole("HR")
        val rootDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "HQ"))
        val childDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "HQ Child", parentId = rootDepartment.id))
        val externalDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "External"))

        employeeRepository.saveAndFlush(
            com.example.employee.person.EmployeeEntity(
                userId = hrUser.id,
                departmentId = rootDepartment.id,
                firstName = "Hr",
                lastName = "Owner",
            ),
        )
        employeeRepository.saveAndFlush(
            com.example.employee.person.EmployeeEntity(
                departmentId = childDepartment.id,
                firstName = "Child",
                lastName = "Employee",
            ),
        )
        val externalEmployee =
            employeeRepository.saveAndFlush(
                com.example.employee.person.EmployeeEntity(
                    departmentId = externalDepartment.id,
                    firstName = "External",
                    lastName = "Employee",
                ),
            )

        val response =
            mockMvc
                .perform(
                    get("/employees")
                        .header("X-Auth-Token", hrUser.id.toString()),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn()

        org.junit.jupiter.api.Assertions
            .assertFalse(response.response.contentAsString.contains(externalEmployee.id.toString()))

        mockMvc
            .perform(
                get("/employees/${externalEmployee.id}")
                    .header("X-Auth-Token", hrUser.id.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `hr sees only departments from own department tree`() {
        val hrUser = createUserWithRole("HR")
        val rootDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "HQ"))
        val childDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "HQ Child", parentId = rootDepartment.id))
        val externalDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "External"))

        employeeRepository.saveAndFlush(
            com.example.employee.person.EmployeeEntity(
                userId = hrUser.id,
                departmentId = rootDepartment.id,
                firstName = "Hr",
                lastName = "Owner",
            ),
        )

        val response =
            mockMvc
                .perform(
                    get("/departments")
                        .header("X-Auth-Token", hrUser.id.toString()),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn()

        org.junit.jupiter.api.Assertions
            .assertFalse(response.response.contentAsString.contains(externalDepartment.id.toString()))

        mockMvc
            .perform(
                get("/departments/${externalDepartment.id}")
                    .header("X-Auth-Token", hrUser.id.toString()),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(
                get("/departments/${childDepartment.id}")
                    .header("X-Auth-Token", hrUser.id.toString()),
            ).andExpect(status().isOk)
    }

    @Test
    fun `write operations create audit records for departments and employees`() {
        val hr = createUserWithRole("HR")

        val departmentResponse =
            mockMvc
                .perform(
                    post("/departments")
                        .header("X-Auth-Token", hr.id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"Audit Dept"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val departmentId = UUID.fromString(extractJsonField(departmentResponse.response.contentAsString, "id"))

        val employeeResponse =
            mockMvc
                .perform(
                    post("/employees")
                        .header("X-Auth-Token", hr.id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "departmentId":"$departmentId",
                              "firstName":"Audit",
                              "lastName":"Employee"
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isCreated)
                .andReturn()

        val employeeId = UUID.fromString(extractJsonField(employeeResponse.response.contentAsString, "id"))

        mockMvc
            .perform(
                put("/employees/$employeeId")
                    .header("X-Auth-Token", hr.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "departmentId":"$departmentId",
                          "firstName":"Audit-upd",
                          "lastName":"Employee"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)

        val departmentLogs = auditLogRepository.findAllByEntityTypeAndEntityId(AuditEntityType.DEPARTMENT, departmentId)
        val employeeLogs = auditLogRepository.findAllByEntityTypeAndEntityId(AuditEntityType.EMPLOYEE, employeeId)

        org.junit.jupiter.api.Assertions
            .assertTrue(departmentLogs.any { it.action == AuditAction.CREATE })
        org.junit.jupiter.api.Assertions
            .assertTrue(employeeLogs.any { it.action == AuditAction.CREATE })
        org.junit.jupiter.api.Assertions
            .assertTrue(employeeLogs.any { it.action == AuditAction.UPDATE })
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
        auditLogRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun extractJsonField(
        json: String,
        field: String,
    ): String {
        val regex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: error("Field '$field' not found in json: $json")
    }
}

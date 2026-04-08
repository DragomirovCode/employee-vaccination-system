package com.example.auth.api

import com.example.audit.log.AuditAction
import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogRepository
import com.example.auth.AuthTestApplication
import com.example.auth.role.RoleEntity
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleEntity
import com.example.auth.role.UserRoleId
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest(classes = [AuthTestApplication::class])
class AuthAdminApiTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        cleanup()
    }

    @Test
    fun `auth admin endpoints require authentication`() {
        mockMvc
            .perform(get("/auth/users"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `non admin cannot create user`() {
        val hrUser = createUserWithRole("HR")
        val session = login(hrUser.email, "hash")

        mockMvc
            .perform(
                post("/auth/users")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"new@example.com","isActive":true}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `admin can create and update user`() {
        val adminUser = createUserWithRole("ADMIN")
        val session = login(adminUser.email, "hash")

        val createdResponse =
            mockMvc
                .perform(
                    post("/auth/users")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"email":"new-admin@example.com","isActive":true}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.email").value("new-admin@example.com"))
                .andReturn()

        val createdId = UUID.fromString(extractJsonField(createdResponse.response.contentAsString, "id"))

        mockMvc
            .perform(
                put("/auth/users/$createdId")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"updated-admin@example.com","isActive":false}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("updated-admin@example.com"))
            .andExpect(jsonPath("$.isActive").value(false))
    }

    @Test
    fun `duplicate email returns conflict`() {
        val adminUser = createUserWithRole("ADMIN")
        val session = login(adminUser.email, "hash")
        userRepository.saveAndFlush(UserEntity(email = "dup@example.com", passwordHash = "hash"))

        mockMvc
            .perform(
                post("/auth/users")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"dup@example.com","isActive":true}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    @Test
    fun `assign role duplicate returns conflict`() {
        val adminUser = createUserWithRole("ADMIN")
        val session = login(adminUser.email, "hash")
        val targetUser = userRepository.saveAndFlush(UserEntity(email = "target@example.com", passwordHash = "hash"))
        val personRole = ensureRole("PERSON")
        userRoleRepository.saveAndFlush(
            UserRoleEntity(id = UserRoleId(userId = targetUser.id, roleId = personRole.id), assignedBy = adminUser.id),
        )

        mockMvc
            .perform(
                post("/auth/users/${targetUser.id}/roles/PERSON")
                    .session(session),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    @Test
    fun `assign and unassign role by admin`() {
        val adminUser = createUserWithRole("ADMIN")
        val session = login(adminUser.email, "hash")
        val targetUser = userRepository.saveAndFlush(UserEntity(email = "target2@example.com", passwordHash = "hash"))
        ensureRole("HR")

        mockMvc
            .perform(
                post("/auth/users/${targetUser.id}/roles/HR")
                    .session(session),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                get("/auth/users/${targetUser.id}/roles")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc
            .perform(
                delete("/auth/users/${targetUser.id}/roles/HR")
                    .session(session),
            ).andExpect(status().isNoContent)
    }

    @Test
    fun `auth admin write operations create audit records`() {
        val adminUser = createUserWithRole("ADMIN")
        val session = login(adminUser.email, "hash")
        ensureRole("PERSON")

        val createResponse =
            mockMvc
                .perform(
                    post("/auth/users")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"email":"audit-auth@example.com","isActive":true}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val createdId = UUID.fromString(extractJsonField(createResponse.response.contentAsString, "id"))

        mockMvc
            .perform(
                patch("/auth/users/$createdId/status")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"active":false}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                post("/auth/users/$createdId/roles/PERSON")
                    .session(session),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                delete("/auth/users/$createdId/roles/PERSON")
                    .session(session),
            ).andExpect(status().isNoContent)

        val userLogs = auditLogRepository.findAllByEntityTypeAndEntityId(AuditEntityType.USER, createdId)
        val allLogs = auditLogRepository.findAll()
        org.junit.jupiter.api.Assertions
            .assertTrue(userLogs.any { it.action == AuditAction.CREATE })
        org.junit.jupiter.api.Assertions
            .assertTrue(userLogs.any { it.action == AuditAction.UPDATE })
        org.junit.jupiter.api.Assertions.assertTrue(
            allLogs.any {
                it.entityType == AuditEntityType.USER_ROLE &&
                    it.action == AuditAction.CREATE
            },
        )
        org.junit.jupiter.api.Assertions.assertTrue(
            allLogs.any {
                it.entityType == AuditEntityType.USER_ROLE &&
                    it.action == AuditAction.DELETE
            },
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
        auditLogRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun login(
        email: String,
        password: String,
    ): MockHttpSession =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"$email","password":"$password"}"""),
            ).andExpect(status().isOk)
            .andReturn()
            .request
            .session as MockHttpSession

    private fun extractJsonField(
        json: String,
        field: String,
    ): String {
        val regex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: error("Field '$field' not found in json: $json")
    }
}

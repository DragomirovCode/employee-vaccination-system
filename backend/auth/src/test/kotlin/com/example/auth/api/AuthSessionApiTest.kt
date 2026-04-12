package com.example.auth.api

import com.example.auth.AuthTestApplication
import com.example.auth.role.RoleEntity
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleEntity
import com.example.auth.role.UserRoleId
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(classes = [AuthTestApplication::class])
class AuthSessionApiTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        val builder = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        builder.apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
        mockMvc = builder.build()
        cleanup()
    }

    @Test
    fun `login creates session and me resolves current user from it`() {
        val user = createUserWithRole("session-user@example.com", "HR")

        val loginResult =
            mockMvc
                .perform(
                    post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"email":"${user.email}","password":"hash"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.userId").value(user.id.toString()))
                .andExpect(jsonPath("$.roles[0]").value("HR"))
                .andReturn()

        val session = loginResult.request.session as MockHttpSession
        assertFalse(session.isInvalid)
        assertNotNull(session.getAttribute("SPRING_SECURITY_CONTEXT"))

        mockMvc
            .perform(
                get("/auth/me")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(user.id.toString()))
            .andExpect(jsonPath("$.roles[0]").value("HR"))
    }

    @Test
    fun `logout invalidates session and protected endpoints require login again`() {
        val user = createUserWithRole("logout-user@example.com", "ADMIN")
        val session = login(user.email, "hash")

        mockMvc
            .perform(
                post("/auth/logout")
                    .session(session),
            ).andExpect(status().isNoContent)
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("JSESSIONID=")))

        mockMvc
            .perform(
                get("/auth/me"),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `cors preflight allows frontend credentials for auth endpoints`() {
        mockMvc
            .perform(
                options("/auth/login")
                    .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-trace-id"),
            ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
    }

    @Test
    fun `insufficient role returns forbidden while unauthenticated requests return unauthorized`() {
        val hrUser = createUserWithRole("hr-user@example.com", "HR")
        val session = login(hrUser.email, "hash")

        mockMvc
            .perform(get("/auth/users"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc
            .perform(
                get("/auth/users")
                    .session(session),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
    }

    private fun cleanup() {
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun createUserWithRole(
        email: String,
        roleCode: String,
    ): UserEntity {
        val user = userRepository.saveAndFlush(UserEntity(email = email, passwordHash = "hash"))
        val role = roleRepository.findByCode(roleCode) ?: roleRepository.saveAndFlush(RoleEntity(code = roleCode, name = roleCode))
        userRoleRepository.saveAndFlush(UserRoleEntity(id = UserRoleId(userId = user.id, roleId = role.id)))
        return user
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
}

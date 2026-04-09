package com.example.auth.api

import com.example.auth.AuthTestApplication
import com.example.auth.notification.NotificationEntity
import com.example.auth.notification.NotificationRepository
import com.example.auth.notification.NotificationType
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant

@SpringBootTest(classes = [AuthTestApplication::class])
class NotificationApiTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

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
    fun `notifications endpoints require authentication`() {
        mockMvc
            .perform(get("/notifications"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `user sees only own notifications and onlyUnread filter works`() {
        val userA = userRepository.saveAndFlush(UserEntity(email = "notify-a@example.com", passwordHash = "hash"))
        val userB = userRepository.saveAndFlush(UserEntity(email = "notify-b@example.com", passwordHash = "hash"))
        val session = login(userA.email, "hash")

        notificationRepository.saveAndFlush(
            NotificationEntity(
                userId = userA.id,
                type = NotificationType.SYSTEM,
                title = "Unread A",
                message = "Unread A",
            ),
        )
        notificationRepository.saveAndFlush(
            NotificationEntity(
                userId = userA.id,
                type = NotificationType.SYSTEM,
                title = "Read A",
                message = "Read A",
                isRead = true,
                readAt = Instant.now(),
            ),
        )
        notificationRepository.saveAndFlush(
            NotificationEntity(
                userId = userB.id,
                type = NotificationType.SYSTEM,
                title = "Unread B",
                message = "Unread B",
            ),
        )

        mockMvc
            .perform(
                get("/notifications")
                    .session(session)
                    .param("page", "0")
                    .param("size", "20"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))

        mockMvc
            .perform(
                get("/notifications")
                    .session(session)
                    .param("onlyUnread", "true")
                    .param("page", "0")
                    .param("size", "20"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Unread A"))
    }

    @Test
    fun `user can mark own notification as read`() {
        val user = userRepository.saveAndFlush(UserEntity(email = "notify-read@example.com", passwordHash = "hash"))
        val session = login(user.email, "hash")
        val notification =
            notificationRepository.saveAndFlush(
                NotificationEntity(
                    userId = user.id,
                    type = NotificationType.SYSTEM,
                    title = "Mark me",
                    message = "Mark me",
                ),
            )

        mockMvc
            .perform(
                patch("/notifications/${notification.id}/read")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.isRead").value(true))

        val updated = notificationRepository.findById(notification.id!!).orElseThrow()
        org.junit.jupiter.api.Assertions
            .assertEquals(true, updated.isRead)
        org.junit.jupiter.api.Assertions
            .assertNotNull(updated.readAt)
    }

    @Test
    fun `user cannot mark another user's notification`() {
        val userA = userRepository.saveAndFlush(UserEntity(email = "notify-own-a@example.com", passwordHash = "hash"))
        val userB = userRepository.saveAndFlush(UserEntity(email = "notify-own-b@example.com", passwordHash = "hash"))
        val session = login(userA.email, "hash")
        val notification =
            notificationRepository.saveAndFlush(
                NotificationEntity(
                    userId = userB.id,
                    type = NotificationType.SYSTEM,
                    title = "B only",
                    message = "B only",
                ),
            )

        mockMvc
            .perform(
                patch("/notifications/${notification.id}/read")
                    .session(session),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `user can mark all own notifications as read`() {
        val user = userRepository.saveAndFlush(UserEntity(email = "notify-bulk@example.com", passwordHash = "hash"))
        val session = login(user.email, "hash")

        notificationRepository.saveAndFlush(
            NotificationEntity(
                userId = user.id,
                type = NotificationType.SYSTEM,
                title = "Bulk 1",
                message = "Bulk 1",
            ),
        )
        notificationRepository.saveAndFlush(
            NotificationEntity(
                userId = user.id,
                type = NotificationType.SYSTEM,
                title = "Bulk 2",
                message = "Bulk 2",
            ),
        )

        mockMvc
            .perform(
                patch("/notifications/read-all")
                    .session(session),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.updated").value(2))

        val unread =
            notificationRepository.findByUserIdAndIsReadFalse(
                userId = user.id!!,
                pageable =
                    org.springframework.data.domain.PageRequest
                        .of(0, 10),
            )
        org.junit.jupiter.api.Assertions
            .assertEquals(0, unread.totalElements)
    }

    private fun cleanup() {
        notificationRepository.deleteAll()
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
}

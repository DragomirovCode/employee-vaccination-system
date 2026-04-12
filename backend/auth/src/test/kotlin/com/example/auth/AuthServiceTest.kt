package com.example.auth

import com.example.auth.role.RoleEntity
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleEntity
import com.example.auth.role.UserRoleId
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException

@SpringBootTest(classes = [AuthTestApplication::class])
class AuthServiceTest {
    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @BeforeEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `authenticates user by email and password and resolves roles`() {
        val user = userRepository.saveAndFlush(UserEntity(email = "auth-ok@example.com", passwordHash = "secret"))
        val role = roleRepository.saveAndFlush(RoleEntity(code = "HR", name = "HR"))
        userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = UserRoleId(userId = user.id, roleId = role.id),
            ),
        )

        val authentication = authService.authenticate("auth-ok@example.com", "secret")
        SecurityContextHolder.getContext().authentication = authentication

        val principal = authService.requireAnyRole(setOf(AppRole.HR, AppRole.ADMIN))

        assertEquals(user.id, principal.userId)
        assertEquals(setOf(AppRole.HR), principal.roles)
    }

    @Test
    fun `rejects invalid credentials`() {
        userRepository.saveAndFlush(UserEntity(email = "auth-bad@example.com", passwordHash = "secret"))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                authService.authenticate("auth-bad@example.com", "wrong-password")
            }

        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
    }

    @Test
    fun `forbids user without required role`() {
        val user = userRepository.saveAndFlush(UserEntity(email = "auth-person@example.com", passwordHash = "secret"))
        val role = roleRepository.saveAndFlush(RoleEntity(code = "PERSON", name = "PERSON"))
        userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = UserRoleId(userId = user.id, roleId = role.id),
            ),
        )

        val authentication = authService.authenticate("auth-person@example.com", "secret")
        SecurityContextHolder.getContext().authentication = authentication

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                authService.requireAnyRole(setOf(AppRole.HR, AppRole.ADMIN))
            }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }
}

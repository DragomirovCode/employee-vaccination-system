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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
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

    @Test
    fun `authenticates user by UUID token and resolves roles`() {
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()

        val user = userRepository.saveAndFlush(UserEntity(email = "auth-ok@example.com", passwordHash = "hash"))
        val role = roleRepository.saveAndFlush(RoleEntity(code = "HR", name = "HR"))
        userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = UserRoleId(userId = user.id, roleId = role.id),
            ),
        )

        val principal = authService.requireAnyRole(user.id.toString(), setOf(AppRole.HR, AppRole.ADMIN))

        assertEquals(user.id, principal.userId)
        assertEquals(setOf(AppRole.HR), principal.roles)
    }

    @Test
    fun `rejects invalid token`() {
        val ex =
            assertThrows(ResponseStatusException::class.java) {
                authService.requireAuthenticated("dev-token")
            }

        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
    }

    @Test
    fun `forbids user without required role`() {
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()

        val user = userRepository.saveAndFlush(UserEntity(email = "auth-person@example.com", passwordHash = "hash"))
        val role = roleRepository.saveAndFlush(RoleEntity(code = "PERSON", name = "PERSON"))
        userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = UserRoleId(userId = user.id, roleId = role.id),
            ),
        )

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                authService.requireAnyRole(user.id.toString(), setOf(AppRole.HR, AppRole.ADMIN))
            }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }
}

package com.example.auth.role

import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException

@SpringBootTest
class RoleAndUserRoleRepositoryTest {
    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `create role and assign to existing user`() {
        val user =
            userRepository.saveAndFlush(
                UserEntity(
                    email = "role-user@example.com",
                    passwordHash = "hash",
                ),
            )
        val role =
            roleRepository.saveAndFlush(
                RoleEntity(
                    code = "PERSON",
                    name = "Employee",
                ),
            )

        userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id =
                    UserRoleId(
                        userId = user.id,
                        roleId = role.id,
                    ),
            ),
        )

        val rolesForUser = userRoleRepository.findAllByIdUserId(user.id!!)

        assertEquals(1, rolesForUser.size)
        assertTrue(roleRepository.existsByCode("PERSON"))
        assertEquals(role.id, rolesForUser.first().id.roleId)
    }

    @Test
    fun `duplicate user role pair is rejected by composite primary key`() {
        val user =
            userRepository.saveAndFlush(
                UserEntity(
                    email = "dup-role-user@example.com",
                    passwordHash = "hash",
                ),
            )
        val role =
            roleRepository.saveAndFlush(
                RoleEntity(
                    code = "HR",
                    name = "HR",
                ),
            )
        val id =
            UserRoleId(
                userId = user.id,
                roleId = role.id,
            )

        userRoleRepository.saveAndFlush(UserRoleEntity(id = id))

        assertThrows<DataIntegrityViolationException> {
            userRoleRepository.saveAndFlush(UserRoleEntity(id = id))
        }
    }

    @Test
    fun `assignment with missing user fails by foreign key`() {
        val role =
            roleRepository.saveAndFlush(
                RoleEntity(
                    code = "ADMIN",
                    name = "Administrator",
                ),
            )

        assertThrows<DataIntegrityViolationException> {
            userRoleRepository.saveAndFlush(
                UserRoleEntity(
                    id =
                        UserRoleId(
                            userId = java.util.UUID.randomUUID(),
                            roleId = role.id,
                        ),
                ),
            )
        }
    }
}

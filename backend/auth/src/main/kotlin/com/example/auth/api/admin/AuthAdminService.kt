package com.example.auth.api.admin

import com.example.auth.role.RoleEntity
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleEntity
import com.example.auth.role.UserRoleId
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class AuthAdminService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository,
) {
    @Transactional(readOnly = true)
    fun listUsers(): List<UserEntity> = userRepository.findAll()

    @Transactional(readOnly = true)
    fun getUser(id: UUID): UserEntity = findUser(id)

    @Transactional
    fun createUser(command: CreateUserCommand): UserEntity {
        requireUniqueEmail(command.email, null)
        return userRepository.saveAndFlush(
            UserEntity(
                email = command.email.trim(),
                passwordHash = command.passwordHash,
                isActive = command.isActive,
            ),
        )
    }

    @Transactional
    fun updateUser(
        id: UUID,
        command: UpdateUserCommand,
    ): UserEntity {
        val user = findUser(id)
        requireUniqueEmail(command.email, id)
        user.email = command.email.trim()
        user.passwordHash = command.passwordHash
        user.isActive = command.isActive
        return userRepository.saveAndFlush(user)
    }

    @Transactional
    fun setStatus(
        id: UUID,
        isActive: Boolean,
    ): UserEntity {
        val user = findUser(id)
        user.isActive = isActive
        return userRepository.saveAndFlush(user)
    }

    @Transactional(readOnly = true)
    fun listRoles(): List<RoleEntity> = roleRepository.findAll()

    @Transactional(readOnly = true)
    fun listUserRoles(userId: UUID): List<UserRoleEntity> {
        findUser(userId)
        return userRoleRepository.findAllByIdUserId(userId)
    }

    @Transactional
    fun assignRole(
        userId: UUID,
        roleCode: String,
        assignedBy: UUID,
    ): UserRoleEntity {
        findUser(userId)
        val role = findRoleByCode(roleCode)
        val roleId = role.id ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")
        val userRoleId = UserRoleId(userId = userId, roleId = roleId)

        if (userRoleRepository.existsById(userRoleId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Role already assigned")
        }

        return userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = userRoleId,
                assignedBy = assignedBy,
            ),
        )
    }

    @Transactional
    fun unassignRole(
        userId: UUID,
        roleCode: String,
    ) {
        findUser(userId)
        val role = findRoleByCode(roleCode)
        val roleId = role.id ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")
        val userRoleId = UserRoleId(userId = userId, roleId = roleId)

        if (!userRoleRepository.existsById(userRoleId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Role assignment not found")
        }
        userRoleRepository.deleteById(userRoleId)
    }

    private fun requireUniqueEmail(
        email: String,
        currentUserId: UUID?,
    ) {
        val existing = userRepository.findByEmail(email.trim())
        if (existing != null && existing.id != currentUserId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already exists")
        }
    }

    private fun findUser(id: UUID): UserEntity =
        userRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

    private fun findRoleByCode(code: String): RoleEntity =
        roleRepository.findByCode(code.trim().uppercase())
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")
}

data class CreateUserCommand(
    val email: String,
    val passwordHash: String,
    val isActive: Boolean,
)

data class UpdateUserCommand(
    val email: String,
    val passwordHash: String,
    val isActive: Boolean,
)

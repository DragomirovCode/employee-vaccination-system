package com.example.auth.api.admin

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
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
    private val auditLogService: AuditLogService,
) {
    @Transactional(readOnly = true)
    fun listUsers(): List<UserEntity> = userRepository.findAll()

    @Transactional(readOnly = true)
    fun getUser(id: UUID): UserEntity = findUser(id)

    @Transactional
    fun createUser(
        command: CreateUserCommand,
        performedBy: UUID,
    ): UserEntity {
        requireUniqueEmail(command.email, null)
        val saved =
            userRepository.saveAndFlush(
            UserEntity(
                email = command.email.trim(),
                passwordHash = command.passwordHash,
                isActive = command.isActive,
            ),
        )
        auditLogService.logCreate(
            userId = performedBy,
            entityType = AuditEntityType.USER,
            entityId = saved.id!!,
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    @Transactional
    fun updateUser(
        id: UUID,
        command: UpdateUserCommand,
        performedBy: UUID,
    ): UserEntity {
        val user = findUser(id)
        val oldPayload = user.toAuditPayload()
        requireUniqueEmail(command.email, id)
        user.email = command.email.trim()
        user.passwordHash = command.passwordHash
        user.isActive = command.isActive
        val saved = userRepository.saveAndFlush(user)
        auditLogService.logUpdate(
            userId = performedBy,
            entityType = AuditEntityType.USER,
            entityId = saved.id!!,
            oldValue = oldPayload,
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    @Transactional
    fun setStatus(
        id: UUID,
        isActive: Boolean,
        performedBy: UUID,
    ): UserEntity {
        val user = findUser(id)
        val oldPayload = user.toAuditPayload()
        user.isActive = isActive
        val saved = userRepository.saveAndFlush(user)
        auditLogService.logUpdate(
            userId = performedBy,
            entityType = AuditEntityType.USER,
            entityId = saved.id!!,
            oldValue = oldPayload,
            newValue = saved.toAuditPayload(),
        )
        return saved
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

        val saved =
            userRoleRepository.saveAndFlush(
            UserRoleEntity(
                id = userRoleId,
                assignedBy = assignedBy,
            ),
        )
        auditLogService.logCreate(
            userId = assignedBy,
            entityType = AuditEntityType.USER_ROLE,
            entityKey = "${userRoleId.userId}:${userRoleId.roleId}",
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    @Transactional
    fun unassignRole(
        userId: UUID,
        roleCode: String,
        unassignedBy: UUID,
    ) {
        findUser(userId)
        val role = findRoleByCode(roleCode)
        val roleId = role.id ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")
        val userRoleId = UserRoleId(userId = userId, roleId = roleId)

        if (!userRoleRepository.existsById(userRoleId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Role assignment not found")
        }
        userRoleRepository.deleteById(userRoleId)
        auditLogService.logDelete(
            userId = unassignedBy,
            entityType = AuditEntityType.USER_ROLE,
            entityKey = "${userRoleId.userId}:${userRoleId.roleId}",
            oldValue = mapOf("userId" to userRoleId.userId.toString(), "roleId" to userRoleId.roleId),
        )
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

    private fun UserEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "id" to id?.toString(),
            "email" to email,
            "isActive" to isActive,
            "createdAt" to createdAt?.toString(),
            "updatedAt" to updatedAt?.toString(),
        )

    private fun UserRoleEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "userId" to id.userId?.toString(),
            "roleId" to id.roleId,
            "assignedAt" to assignedAt?.toString(),
            "assignedBy" to assignedBy?.toString(),
        )
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

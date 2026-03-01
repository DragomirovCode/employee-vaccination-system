package com.example.auth.role

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRoleRepository : JpaRepository<UserRoleEntity, UserRoleId> {
    fun findAllByIdUserId(userId: UUID): List<UserRoleEntity>
}

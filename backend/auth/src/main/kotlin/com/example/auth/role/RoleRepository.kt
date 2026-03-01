package com.example.auth.role

import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<RoleEntity, Int> {
    fun findByCode(code: String): RoleEntity?

    fun existsByCode(code: String): Boolean
}

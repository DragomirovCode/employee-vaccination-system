package com.example.auth.role

import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<RoleEntity, Int> {
    /**
     * Ищет роль по коду.
     */
    fun findByCode(code: String): RoleEntity?

    /**
     * Проверяет наличие роли с указанным кодом.
     */
    fun existsByCode(code: String): Boolean
}

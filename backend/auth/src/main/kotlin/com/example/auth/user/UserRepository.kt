package com.example.auth.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    /**
     * Ищет пользователя по email.
     */
    fun findByEmail(email: String): UserEntity?

    /**
     * Проверяет наличие пользователя с указанным email.
     */
    fun existsByEmail(email: String): Boolean
}

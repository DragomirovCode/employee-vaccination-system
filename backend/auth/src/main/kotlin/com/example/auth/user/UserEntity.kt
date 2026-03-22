package com.example.auth.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
/**
 * JPA-сущность учетной записи пользователя.
 */
class UserEntity(
    /** Уникальный идентификатор пользователя. */
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,
    /** Email пользователя, используемый как логин. */
    @Column(nullable = false, unique = true)
    var email: String = "",
    /** Хэш пароля пользователя. */
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",
    /** Признак того, что учетная запись активна. */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    /** Момент создания учетной записи. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    /** Момент последнего обновления учетной записи. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    /**
     * Заполняет идентификатор и временные метки перед первой вставкой пользователя.
     */
    @PrePersist
    fun onCreate() {
        if (id == null) {
            id = UuidV7Generator.next()
        }
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    /**
     * Обновляет время последнего изменения учетной записи перед сохранением.
     */
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

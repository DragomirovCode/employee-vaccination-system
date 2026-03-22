package com.example.auth.role

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * JPA-сущность роли пользователя.
 */
@Entity
@Table(name = "roles")
class RoleEntity(
    /** Числовой идентификатор роли. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    /** Машинный код роли. */
    @Column(nullable = false, unique = true, length = 64)
    var code: String = "",
    /** Человекочитаемое название роли. */
    @Column(nullable = false, length = 255)
    var name: String = "",
)

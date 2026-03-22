package com.example.auth.role

import com.example.auth.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_roles")
/**
 * JPA-сущность назначения роли пользователю.
 */
class UserRoleEntity(
    /** Составной идентификатор назначения роли. */
    @EmbeddedId
    var id: UserRoleId = UserRoleId(),
    /** Связанный пользователь. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: UserEntity? = null,
    /** Связанная роль. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    var role: RoleEntity? = null,
    /** Момент назначения роли пользователю. */
    @Column(name = "assigned_at", nullable = false)
    var assignedAt: Instant? = null,
    /** Идентификатор пользователя, выполнившего назначение роли. */
    @Column(name = "assigned_by")
    var assignedBy: UUID? = null,
) {
    /**
     * Заполняет дату назначения перед первой вставкой записи в базу данных.
     */
    @PrePersist
    fun onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now()
        }
    }
}

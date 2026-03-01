package com.example.auth.role

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class UserRoleId(
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,
    @Column(name = "role_id", nullable = false)
    var roleId: Int? = null,
) : Serializable

package com.example.auth.role

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRoleRepository : JpaRepository<UserRoleEntity, UserRoleId> {
    fun findAllByIdUserId(userId: UUID): List<UserRoleEntity>

    @Query(
        """
        select r.code
        from UserRoleEntity ur
        join ur.role r
        where ur.id.userId = :userId
    """,
    )
    fun findRoleCodesByUserId(
        @Param("userId") userId: UUID,
    ): List<String>
}

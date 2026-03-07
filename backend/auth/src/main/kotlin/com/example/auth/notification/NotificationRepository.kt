package com.example.auth.notification

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface NotificationRepository : JpaRepository<NotificationEntity, UUID> {
    fun findByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<NotificationEntity>

    fun findByUserIdAndIsReadFalse(
        userId: UUID,
        pageable: Pageable,
    ): Page<NotificationEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): NotificationEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE NotificationEntity n
        SET n.isRead = true, n.readAt = :readAt
        WHERE n.userId = :userId AND n.isRead = false
        """,
    )
    fun markAllAsRead(
        @Param("userId") userId: UUID,
        @Param("readAt") readAt: Instant,
    ): Int
}

package com.example.auth.notification

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    @Transactional
    fun create(command: CreateNotificationCommand): NotificationEntity =
        notificationRepository.saveAndFlush(
            NotificationEntity(
                userId = command.userId,
                type = command.type,
                title = command.title,
                message = command.message,
                payload = command.payload,
            ),
        )

    @Transactional(readOnly = true)
    fun listForUser(
        userId: UUID,
        onlyUnread: Boolean,
        pageable: Pageable,
    ): Page<NotificationEntity> =
        if (onlyUnread) {
            notificationRepository.findByUserIdAndIsReadFalse(userId, pageable)
        } else {
            notificationRepository.findByUserId(userId, pageable)
        }

    @Transactional
    fun markRead(
        userId: UUID,
        notificationId: UUID,
    ): NotificationEntity {
        val notification =
            notificationRepository.findByIdAndUserId(notificationId, userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found")

        if (!notification.isRead) {
            notification.isRead = true
            notification.readAt = Instant.now()
            notificationRepository.saveAndFlush(notification)
        }

        return notification
    }

    @Transactional
    fun markAllRead(userId: UUID): Int = notificationRepository.markAllAsRead(userId, Instant.now())
}

data class CreateNotificationCommand(
    val userId: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val payload: String? = null,
)

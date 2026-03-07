package com.example.auth.api.notification

import com.example.auth.AuthenticatedPrincipal
import com.example.auth.api.ApiErrorResponse
import com.example.auth.notification.NotificationEntity
import com.example.auth.notification.NotificationService
import com.example.auth.notification.NotificationType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Personal notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping
    @Operation(summary = "Get current user's notifications")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Notifications page",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = NotificationResponse::class)),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun list(
        request: HttpServletRequest,
        @Parameter(description = "Return unread only", example = "false")
        @RequestParam(defaultValue = "false")
        onlyUnread: Boolean,
        @RequestParam(defaultValue = "0")
        page: Int,
        @RequestParam(defaultValue = "20")
        size: Int,
    ): Page<NotificationResponse> {
        val principal = requirePrincipal(request)
        return notificationService
            .listForUser(
                userId = principal.userId,
                onlyUnread = onlyUnread,
                pageable = PageRequest.of(page, size),
            ).map(NotificationResponse::fromEntity)
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Notification marked as read"),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Notification not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun markRead(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ): NotificationResponse {
        val principal = requirePrincipal(request)
        return NotificationResponse.fromEntity(notificationService.markRead(principal.userId, id))
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all current user's notifications as read")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Notifications marked as read"),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun markAllRead(request: HttpServletRequest): NotificationBulkReadResponse {
        val principal = requirePrincipal(request)
        val updated = notificationService.markAllRead(principal.userId)
        return NotificationBulkReadResponse(updated = updated)
    }

    private fun requirePrincipal(request: HttpServletRequest): AuthenticatedPrincipal =
        request.getAttribute(NotificationSecurityContext.PRINCIPAL_ATTRIBUTE) as? AuthenticatedPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing security principal")
}

data class NotificationResponse(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Instant,
    val readAt: Instant?,
    val payload: String?,
) {
    companion object {
        fun fromEntity(entity: NotificationEntity): NotificationResponse =
            NotificationResponse(
                id = entity.id!!,
                type = entity.type,
                title = entity.title,
                message = entity.message,
                isRead = entity.isRead,
                createdAt = entity.createdAt!!,
                readAt = entity.readAt,
                payload = entity.payload,
            )
    }
}

data class NotificationBulkReadResponse(
    val updated: Int,
)

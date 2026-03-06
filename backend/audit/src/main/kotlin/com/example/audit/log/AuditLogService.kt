package com.example.audit.log

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
) {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Transactional
    fun logCreate(
        userId: UUID,
        entityType: AuditEntityType,
        entityId: UUID,
        newValue: Any?,
    ) {
        save(
            userId = userId,
            action = AuditAction.CREATE,
            entityType = entityType,
            entityId = entityId,
            oldValue = null,
            newValue = toJson(newValue),
        )
    }

    @Transactional
    fun logUpdate(
        userId: UUID,
        entityType: AuditEntityType,
        entityId: UUID,
        oldValue: Any?,
        newValue: Any?,
    ) {
        save(
            userId = userId,
            action = AuditAction.UPDATE,
            entityType = entityType,
            entityId = entityId,
            oldValue = toJson(oldValue),
            newValue = toJson(newValue),
        )
    }

    @Transactional
    fun logDelete(
        userId: UUID,
        entityType: AuditEntityType,
        entityId: UUID,
        oldValue: Any?,
    ) {
        save(
            userId = userId,
            action = AuditAction.DELETE,
            entityType = entityType,
            entityId = entityId,
            oldValue = toJson(oldValue),
            newValue = null,
        )
    }

    private fun save(
        userId: UUID,
        action: AuditAction,
        entityType: AuditEntityType,
        entityId: UUID,
        oldValue: String?,
        newValue: String?,
    ) {
        auditLogRepository.saveAndFlush(
            AuditLogEntity(
                userId = userId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                oldValue = oldValue,
                newValue = newValue,
            ),
        )
    }

    private fun toJson(value: Any?): String? =
        value?.let {
            objectMapper.writeValueAsString(it)
        }
}

package com.example.audit.log

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuditLogRepository : JpaRepository<AuditLogEntity, UUID> {
    fun findAllByEntityTypeAndEntityId(
        entityType: AuditEntityType,
        entityId: UUID,
    ): List<AuditLogEntity>
}

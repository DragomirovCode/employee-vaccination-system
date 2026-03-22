package com.example.audit.log

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuditLogRepository : JpaRepository<AuditLogEntity, UUID> {
    /**
     * Возвращает все записи аудита, относящиеся к конкретной сущности.
     *
     * @param entityType тип сущности
     * @param entityId идентификатор сущности
     * @return список записей аудита для указанной сущности
     */
    fun findAllByEntityTypeAndEntityId(
        entityType: AuditEntityType,
        entityId: UUID,
    ): List<AuditLogEntity>
}

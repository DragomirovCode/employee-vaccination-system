package com.example.audit.log

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
) {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    /**
     * Сохраняет запись аудита о создании сущности с UUID-идентификатором.
     *
     * @param userId идентификатор пользователя, выполнившего операцию
     * @param entityType тип измененной сущности
     * @param entityId идентификатор созданной сущности
     * @param newValue состояние сущности после создания
     */
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

    /**
     * Сохраняет запись аудита о создании сущности, если у нее нет собственного UUID
     * и для аудита используется синтетический идентификатор на основе ключа.
     *
     * @param userId идентификатор пользователя, выполнившего операцию
     * @param entityType тип измененной сущности
     * @param entityKey строковый ключ сущности, из которого формируется синтетический UUID
     * @param newValue состояние сущности после создания
     */
    @Transactional
    fun logCreate(
        userId: UUID,
        entityType: AuditEntityType,
        entityKey: String,
        newValue: Any?,
    ) {
        logCreate(
            userId = userId,
            entityType = entityType,
            entityId = syntheticEntityId(entityType, entityKey),
            newValue = newValue,
        )
    }

    /**
     * Сохраняет запись аудита об обновлении сущности с UUID-идентификатором.
     *
     * @param userId идентификатор пользователя, выполнившего операцию
     * @param entityType тип измененной сущности
     * @param entityId идентификатор измененной сущности
     * @param oldValue состояние сущности до изменения
     * @param newValue состояние сущности после изменения
     */
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

    /**
     * Сохраняет запись аудита об обновлении сущности, если в журнале изменений
     * она должна определяться по строковому ключу.
     *
     * @param userId идентификатор пользователя, выполнившего операцию
     * @param entityType тип измененной сущности
     * @param entityKey строковый ключ сущности, из которого формируется синтетический UUID
     * @param oldValue состояние сущности до изменения
     * @param newValue состояние сущности после изменения
     */
    @Transactional
    fun logUpdate(
        userId: UUID,
        entityType: AuditEntityType,
        entityKey: String,
        oldValue: Any?,
        newValue: Any?,
    ) {
        logUpdate(
            userId = userId,
            entityType = entityType,
            entityId = syntheticEntityId(entityType, entityKey),
            oldValue = oldValue,
            newValue = newValue,
        )
    }

    /**
     * Сохраняет запись аудита об удалении сущности с UUID-идентификатором.
     *
     * @param userId идентификатор пользователя, выполнившего операцию
     * @param entityType тип удаленной сущности
     * @param entityId идентификатор удаленной сущности
     * @param oldValue состояние сущности перед удалением
     */
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

    /**
     * Сохраняет запись аудита об удалении сущности, если для ее идентификации
     * используется строковый ключ вместо собственного UUID.
     *
     * @param userId идентификатор пользователя, выполнившего операцию
     * @param entityType тип удаленной сущности
     * @param entityKey строковый ключ сущности, из которого формируется синтетический UUID
     * @param oldValue состояние сущности перед удалением
     */
    @Transactional
    fun logDelete(
        userId: UUID,
        entityType: AuditEntityType,
        entityKey: String,
        oldValue: Any?,
    ) {
        logDelete(
            userId = userId,
            entityType = entityType,
            entityId = syntheticEntityId(entityType, entityKey),
            oldValue = oldValue,
        )
    }

    /**
     * Создает и немедленно сохраняет запись аудита в базе данных.
     */
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

    /**
     * Сериализует произвольный объект в JSON для хранения в журнале аудита.
     *
     * @param value значение для сериализации
     * @return JSON-представление значения или `null`, если значение отсутствует
     */
    private fun toJson(value: Any?): String? =
        value?.let {
            objectMapper.writeValueAsString(it)
        }

    /**
     * Строит детерминированный UUID для сущностей, которые идентифицируются строковым ключом.
     *
     * @param entityType тип сущности
     * @param entityKey строковый ключ сущности
     * @return синтетический UUID, одинаковый для одной и той же пары типа и ключа
     */
    private fun syntheticEntityId(
        entityType: AuditEntityType,
        entityKey: String,
    ): UUID = UUID.nameUUIDFromBytes("${entityType.name}:$entityKey".toByteArray(StandardCharsets.UTF_8))
}

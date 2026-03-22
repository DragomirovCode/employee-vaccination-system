package com.example.audit

import com.example.audit.log.AuditAction
import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogEntity
import com.example.audit.log.AuditLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest(classes = [AuditTestApplication::class])
class AuditLogRepositoryTest {
    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    /**
     * Проверяет, что запись аудита сохраняется в базе и затем находится по типу и идентификатору сущности.
     */
    @Test
    fun `save and query audit log`() {
        auditLogRepository.deleteAll()

        val userId = UUID.randomUUID()
        val entityId = UUID.randomUUID()
        val saved =
            auditLogRepository.saveAndFlush(
                AuditLogEntity(
                    userId = userId,
                    action = AuditAction.CREATE,
                    entityType = AuditEntityType.VACCINATION,
                    entityId = entityId,
                    oldValue = null,
                    newValue = """{"id":"$entityId"}""",
                ),
            )

        assertNotNull(saved.id)
        assertEquals(7, saved.id!!.version())

        val found = auditLogRepository.findAllByEntityTypeAndEntityId(AuditEntityType.VACCINATION, entityId)
        assertEquals(1, found.size)
        assertEquals(AuditAction.CREATE, found.first().action)
    }
}

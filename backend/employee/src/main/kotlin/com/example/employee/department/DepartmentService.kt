package com.example.employee.department

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
import com.example.employee.person.EmployeeRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val departmentAccessScopeResolver: DepartmentAccessScopeResolver,
    private val auditLogService: AuditLogService,
) {
    @Transactional(readOnly = true)
    fun list(principal: com.example.auth.AuthenticatedPrincipal): List<DepartmentEntity> = departmentAccessScopeResolver.list(principal)

    @Transactional(readOnly = true)
    fun get(
        id: UUID,
        principal: com.example.auth.AuthenticatedPrincipal,
    ): DepartmentEntity {
        val department = findDepartment(id)
        if (!departmentAccessScopeResolver.canRead(principal, department)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Requested department is outside access scope")
        }
        return department
    }

    @Transactional
    fun create(
        command: CreateDepartmentCommand,
        performedBy: UUID,
    ): DepartmentEntity {
        requireParentExists(command.parentId)

        val entity =
            DepartmentEntity(
                name = command.name.trim(),
                parentId = command.parentId,
            )
        val saved = departmentRepository.saveAndFlush(entity)
        auditLogService.logCreate(
            userId = performedBy,
            entityType = AuditEntityType.DEPARTMENT,
            entityId = saved.id!!,
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    @Transactional
    fun update(
        id: UUID,
        command: UpdateDepartmentCommand,
        performedBy: UUID,
    ): DepartmentEntity {
        val department = findDepartment(id)
        val oldPayload = department.toAuditPayload()
        requireParentExists(command.parentId)
        requireNoCycle(id, command.parentId)

        department.name = command.name.trim()
        department.parentId = command.parentId
        val saved = departmentRepository.saveAndFlush(department)
        auditLogService.logUpdate(
            userId = performedBy,
            entityType = AuditEntityType.DEPARTMENT,
            entityId = saved.id!!,
            oldValue = oldPayload,
            newValue = saved.toAuditPayload(),
        )
        return saved
    }

    @Transactional
    fun delete(
        id: UUID,
        performedBy: UUID,
    ) {
        val existing = findDepartment(id)
        if (departmentRepository.existsByParentId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Department has child departments")
        }
        if (employeeRepository.existsByDepartmentId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Department has employees")
        }
        departmentRepository.deleteById(id)
        auditLogService.logDelete(
            userId = performedBy,
            entityType = AuditEntityType.DEPARTMENT,
            entityId = id,
            oldValue = existing.toAuditPayload(),
        )
    }

    private fun requireParentExists(parentId: UUID?) {
        if (parentId != null && !departmentRepository.existsById(parentId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "parentId does not exist")
        }
    }

    private fun requireNoCycle(
        departmentId: UUID,
        parentId: UUID?,
    ) {
        if (parentId == null) {
            return
        }
        if (parentId == departmentId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Department hierarchy cycle detected")
        }

        var cursor = parentId
        while (cursor != null) {
            if (cursor == departmentId) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Department hierarchy cycle detected")
            }
            cursor = departmentRepository.findById(cursor).orElse(null)?.parentId
        }
    }

    private fun findDepartment(id: UUID): DepartmentEntity =
        departmentRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found")
        }

    private fun DepartmentEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "id" to id?.toString(),
            "name" to name,
            "parentId" to parentId?.toString(),
            "createdAt" to createdAt?.toString(),
        )
}

data class CreateDepartmentCommand(
    val name: String,
    val parentId: UUID?,
)

data class UpdateDepartmentCommand(
    val name: String,
    val parentId: UUID?,
)

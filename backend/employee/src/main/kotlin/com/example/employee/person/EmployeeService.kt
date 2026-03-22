package com.example.employee.person

import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogService
import com.example.employee.department.DepartmentRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@Service
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val departmentRepository: DepartmentRepository,
    private val employeeAccessScopeResolver: EmployeeAccessScopeResolver,
    private val auditLogService: AuditLogService,
) {
    /**
     * Возвращает список сотрудников, доступных пользователю.
     */
    @Transactional(readOnly = true)
    fun list(principal: com.example.auth.AuthenticatedPrincipal): List<EmployeeEntity> = employeeAccessScopeResolver.list(principal)

    /**
     * Возвращает сотрудника по идентификатору с проверкой прав доступа.
     */
    @Transactional(readOnly = true)
    fun get(
        id: UUID,
        principal: com.example.auth.AuthenticatedPrincipal,
    ): EmployeeEntity {
        val employee = findEmployee(id)
        if (!employeeAccessScopeResolver.canRead(principal, employee)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Requested employee is outside access scope")
        }
        return employee
    }

    /**
     * Создает сотрудника и фиксирует операцию в журнале аудита.
     */
    @Transactional
    fun create(
        command: CreateEmployeeCommand,
        performedBy: UUID,
    ): EmployeeEntity {
        validateDepartment(command.departmentId)
        validateUserUniqueness(command.userId, null)

        return try {
            val saved =
                employeeRepository.saveAndFlush(
                    EmployeeEntity(
                        userId = command.userId,
                        departmentId = command.departmentId,
                        firstName = command.firstName.trim(),
                        lastName = command.lastName.trim(),
                        middleName = command.middleName?.trim(),
                        birthDate = command.birthDate,
                        position = command.position?.trim(),
                        hireDate = command.hireDate,
                    ),
                )
            auditLogService.logCreate(
                userId = performedBy,
                entityType = AuditEntityType.EMPLOYEE,
                entityId = saved.id!!,
                newValue = saved.toAuditPayload(),
            )
            saved
        } catch (ex: DataIntegrityViolationException) {
            throw toConflict(ex)
        }
    }

    /**
     * Обновляет данные сотрудника и сохраняет изменения в аудите.
     */
    @Transactional
    fun update(
        id: UUID,
        command: UpdateEmployeeCommand,
        performedBy: UUID,
    ): EmployeeEntity {
        val employee = findEmployee(id)
        val oldPayload = employee.toAuditPayload()
        validateDepartment(command.departmentId)
        validateUserUniqueness(command.userId, id)

        employee.userId = command.userId
        employee.departmentId = command.departmentId
        employee.firstName = command.firstName.trim()
        employee.lastName = command.lastName.trim()
        employee.middleName = command.middleName?.trim()
        employee.birthDate = command.birthDate
        employee.position = command.position?.trim()
        employee.hireDate = command.hireDate

        return try {
            val saved = employeeRepository.saveAndFlush(employee)
            auditLogService.logUpdate(
                userId = performedBy,
                entityType = AuditEntityType.EMPLOYEE,
                entityId = saved.id!!,
                oldValue = oldPayload,
                newValue = saved.toAuditPayload(),
            )
            saved
        } catch (ex: DataIntegrityViolationException) {
            throw toConflict(ex)
        }
    }

    /**
     * Удаляет сотрудника, если он не связан с учетной записью пользователя
     * и на него не ссылаются зависимые записи.
     */
    @Transactional
    fun delete(
        id: UUID,
        performedBy: UUID,
    ) {
        val existing = findEmployee(id)
        if (existing.userId != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Employee has linked user account")
        }
        try {
            employeeRepository.deleteById(id)
            employeeRepository.flush()
        } catch (ex: DataIntegrityViolationException) {
            throw toDeleteConflict(ex)
        }
        auditLogService.logDelete(
            userId = performedBy,
            entityType = AuditEntityType.EMPLOYEE,
            entityId = id,
            oldValue = existing.toAuditPayload(),
        )
    }

    /**
     * Проверяет существование подразделения, к которому привязывается сотрудник.
     */
    private fun validateDepartment(departmentId: UUID) {
        if (!departmentRepository.existsById(departmentId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "departmentId does not exist")
        }
    }

    /**
     * Проверяет, что пользовательская учетная запись не привязана к другому сотруднику.
     */
    private fun validateUserUniqueness(
        userId: UUID?,
        currentEmployeeId: UUID?,
    ) {
        if (userId == null) {
            return
        }

        val existing = employeeRepository.findByUserId(userId)
        if (existing != null && existing.id != currentEmployeeId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "userId is already assigned to another employee")
        }
    }

    /**
     * Ищет сотрудника по идентификатору или выбрасывает ошибку 404.
     */
    private fun findEmployee(id: UUID): EmployeeEntity =
        employeeRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }

    /**
     * Преобразует ошибку нарушения целостности данных в HTTP-конфликт.
     */
    private fun toConflict(ex: DataIntegrityViolationException): ResponseStatusException {
        val message = ex.rootCause?.message ?: ex.message ?: "Data integrity violation"
        return ResponseStatusException(HttpStatus.CONFLICT, message)
    }

    /**
     * Преобразует ошибку удаления в пользовательский конфликт с более понятным сообщением.
     */
    private fun toDeleteConflict(ex: DataIntegrityViolationException): ResponseStatusException {
        val message = ex.rootCause?.message ?: ex.message ?: "Data integrity violation"
        return if (message.contains("fk_vaccinations_employee", ignoreCase = true)) {
            ResponseStatusException(HttpStatus.CONFLICT, "Employee has vaccination records")
        } else {
            ResponseStatusException(HttpStatus.CONFLICT, message)
        }
    }

    /**
     * Преобразует сотрудника в сериализуемое представление для аудита.
     */
    private fun EmployeeEntity.toAuditPayload(): Map<String, Any?> =
        mapOf(
            "id" to id?.toString(),
            "userId" to userId?.toString(),
            "departmentId" to departmentId?.toString(),
            "firstName" to firstName,
            "lastName" to lastName,
            "middleName" to middleName,
            "birthDate" to birthDate?.toString(),
            "position" to position,
            "hireDate" to hireDate?.toString(),
            "createdAt" to createdAt?.toString(),
            "updatedAt" to updatedAt?.toString(),
        )
}

data class CreateEmployeeCommand(
    val userId: UUID?,
    val departmentId: UUID,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val birthDate: LocalDate?,
    val position: String?,
    val hireDate: LocalDate?,
)

data class UpdateEmployeeCommand(
    val userId: UUID?,
    val departmentId: UUID,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val birthDate: LocalDate?,
    val position: String?,
    val hireDate: LocalDate?,
)

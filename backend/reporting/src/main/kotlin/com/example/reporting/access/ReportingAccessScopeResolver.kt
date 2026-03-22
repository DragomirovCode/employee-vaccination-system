package com.example.reporting.access

import com.example.auth.AppRole
import com.example.auth.AuthenticatedPrincipal
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ReportingAccessScopeResolver(
    private val employeeRepository: EmployeeRepository,
    private val departmentRepository: DepartmentRepository,
) {
    /**
     * Вычисляет область доступа к отчетам с учетом ролей пользователя
     * и необязательного фильтра по подразделению.
     */
    @Transactional(readOnly = true)
    fun resolve(
        principal: AuthenticatedPrincipal,
        requestedDepartmentId: UUID?,
    ): ReportingAccessScope {
        if (principal.hasAnyRole(AppRole.ADMIN, AppRole.MEDICAL)) {
            return resolveFullAccess(requestedDepartmentId)
        }

        if (principal.hasRole(AppRole.HR)) {
            return resolveHrAccess(principal.userId, requestedDepartmentId)
        }

        if (principal.hasRole(AppRole.PERSON)) {
            return resolvePersonAccess(principal.userId, requestedDepartmentId)
        }

        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions")
    }

    /**
     * Возвращает полную область доступа для ролей с неограниченным просмотром отчетов.
     */
    private fun resolveFullAccess(requestedDepartmentId: UUID?): ReportingAccessScope =
        if (requestedDepartmentId == null) {
            ReportingAccessScope()
        } else {
            ReportingAccessScope(departmentIds = setOf(requestedDepartmentId))
        }

    /**
     * Вычисляет область доступа для HR-пользователя:
     * его подразделение и все дочерние, либо поддерево запрошенного подразделения внутри доступного контура.
     */
    private fun resolveHrAccess(
        userId: UUID,
        requestedDepartmentId: UUID?,
    ): ReportingAccessScope {
        val employee =
            employeeRepository.findByUserId(userId)
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")

        val hrDepartmentId =
            employee.departmentId
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee has no department")

        val allDepartments = departmentRepository.findAll()
        val descendants = collectDescendants(hrDepartmentId, allDepartments)

        val effectiveDepartmentIds =
            if (requestedDepartmentId == null) {
                descendants
            } else {
                if (!descendants.contains(requestedDepartmentId)) {
                    throw ResponseStatusException(HttpStatus.FORBIDDEN, "Requested department is outside access scope")
                }
                collectDescendants(requestedDepartmentId, allDepartments)
            }

        return ReportingAccessScope(departmentIds = effectiveDepartmentIds)
    }

    /**
     * Вычисляет область доступа для обычного сотрудника, ограничивая отчеты его собственной карточкой.
     */
    private fun resolvePersonAccess(
        userId: UUID,
        requestedDepartmentId: UUID?,
    ): ReportingAccessScope {
        val employee =
            employeeRepository.findByUserId(userId)
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")

        if (requestedDepartmentId != null && employee.departmentId != requestedDepartmentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Requested department is outside access scope")
        }

        val employeeId =
            employee.id
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee profile is incomplete")

        return ReportingAccessScope(employeeId = employeeId)
    }

    /**
     * Собирает идентификаторы подразделения и всех его потомков.
     */
    private fun collectDescendants(
        rootId: UUID,
        allDepartments: List<com.example.employee.department.DepartmentEntity>,
    ): Set<UUID> {
        val childrenByParent = allDepartments.filter { it.id != null }.groupBy({ it.parentId }, { it.id!! })
        val result = mutableSetOf<UUID>()
        val queue = ArrayDeque<UUID>()
        queue.add(rootId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!result.add(current)) {
                continue
            }
            childrenByParent[current].orEmpty().forEach(queue::addLast)
        }

        return result
    }

    /** Проверяет наличие конкретной роли у пользователя. */
    private fun AuthenticatedPrincipal.hasRole(role: AppRole): Boolean = roles.contains(role)

    /** Проверяет наличие хотя бы одной роли из переданного набора. */
    private fun AuthenticatedPrincipal.hasAnyRole(vararg allowedRoles: AppRole): Boolean = roles.any { it in allowedRoles }
}

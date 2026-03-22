package com.example.employee.person

import com.example.auth.AppRole
import com.example.auth.AuthenticatedPrincipal
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class EmployeeAccessScopeResolver(
    private val employeeRepository: EmployeeRepository,
    private val departmentRepository: DepartmentRepository,
) {
    /**
     * Возвращает список сотрудников, доступных пользователю в зависимости от его ролей.
     */
    @Transactional(readOnly = true)
    fun list(principal: AuthenticatedPrincipal): List<EmployeeEntity> {
        if (principal.hasAnyRole(AppRole.ADMIN, AppRole.MEDICAL)) {
            return employeeRepository.findAll()
        }

        if (principal.hasRole(AppRole.HR)) {
            val departmentIds = resolveHrDepartmentIds(principal.userId)
            return employeeRepository.findAllByDepartmentIdIn(departmentIds)
        }

        if (principal.hasRole(AppRole.PERSON)) {
            return employeeRepository.findByUserId(principal.userId)?.let(::listOf).orEmpty()
        }

        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions")
    }

    /**
     * Проверяет, может ли пользователь читать карточку указанного сотрудника.
     */
    @Transactional(readOnly = true)
    fun canRead(
        principal: AuthenticatedPrincipal,
        employee: EmployeeEntity,
    ): Boolean {
        if (principal.hasAnyRole(AppRole.ADMIN, AppRole.MEDICAL)) {
            return true
        }

        if (principal.hasRole(AppRole.HR)) {
            val employeeDepartmentId = employee.departmentId ?: return false
            return resolveHrDepartmentIds(principal.userId).contains(employeeDepartmentId)
        }

        if (principal.hasRole(AppRole.PERSON)) {
            return employee.userId == principal.userId
        }

        return false
    }

    /**
     * Определяет множество подразделений, доступных HR-пользователю:
     * его собственное подразделение и все дочерние.
     */
    private fun resolveHrDepartmentIds(userId: UUID): Set<UUID> {
        val employee =
            employeeRepository.findByUserId(userId)
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")

        val rootDepartmentId =
            employee.departmentId
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee has no department")

        return collectDescendants(rootDepartmentId, departmentRepository.findAll())
    }

    /**
     * Собирает идентификаторы корневого подразделения и всех его потомков.
     */
    private fun collectDescendants(
        rootId: UUID,
        allDepartments: List<DepartmentEntity>,
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

package com.example.employee.department

import com.example.auth.AppRole
import com.example.auth.AuthenticatedPrincipal
import com.example.employee.person.EmployeeRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class DepartmentAccessScopeResolver(
    private val departmentRepository: DepartmentRepository,
    private val employeeRepository: EmployeeRepository,
) {
    @Transactional(readOnly = true)
    fun list(principal: AuthenticatedPrincipal): List<DepartmentEntity> {
        if (principal.hasAnyRole(AppRole.ADMIN, AppRole.MEDICAL)) {
            return departmentRepository.findAll()
        }

        if (principal.hasRole(AppRole.HR)) {
            val departmentIds = resolveHrDepartmentIds(principal.userId)
            return departmentRepository.findAllById(departmentIds).sortedBy { it.createdAt }
        }

        if (principal.hasRole(AppRole.PERSON)) {
            val departmentId =
                employeeRepository.findByUserId(principal.userId)?.departmentId
                    ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")
            return departmentRepository.findById(departmentId).map(::listOf).orElse(emptyList())
        }

        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions")
    }

    @Transactional(readOnly = true)
    fun canRead(
        principal: AuthenticatedPrincipal,
        department: DepartmentEntity,
    ): Boolean {
        if (principal.hasAnyRole(AppRole.ADMIN, AppRole.MEDICAL)) {
            return true
        }

        if (principal.hasRole(AppRole.HR)) {
            val departmentId = department.id ?: return false
            return resolveHrDepartmentIds(principal.userId).contains(departmentId)
        }

        if (principal.hasRole(AppRole.PERSON)) {
            return employeeRepository.findByUserId(principal.userId)?.departmentId == department.id
        }

        return false
    }

    private fun resolveHrDepartmentIds(userId: UUID): Set<UUID> {
        val employee =
            employeeRepository.findByUserId(userId)
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")

        val rootDepartmentId =
            employee.departmentId
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee has no department")

        return collectDescendants(rootDepartmentId, departmentRepository.findAll())
    }

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

    private fun AuthenticatedPrincipal.hasRole(role: AppRole): Boolean = roles.contains(role)

    private fun AuthenticatedPrincipal.hasAnyRole(vararg allowedRoles: AppRole): Boolean = roles.any { it in allowedRoles }
}

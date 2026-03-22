package com.example.vaccination.api.security

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
class VaccinationReadScopeResolver(
    private val employeeRepository: EmployeeRepository,
    private val departmentRepository: DepartmentRepository,
) {
    /**
     * Вычисляет область чтения записей вакцинации на основе ролей пользователя.
     */
    @Transactional(readOnly = true)
    fun resolve(principal: AuthenticatedPrincipal): VaccinationReadScope {
        if (principal.roles.contains(AppRole.ADMIN) || principal.roles.contains(AppRole.MEDICAL)) {
            return VaccinationReadScope()
        }

        if (principal.roles.contains(AppRole.HR)) {
            val employee =
                employeeRepository.findByUserId(principal.userId)
                    ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")

            val hrDepartmentId =
                employee.departmentId
                    ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee has no department")

            val descendants = collectDescendants(hrDepartmentId)
            return VaccinationReadScope(departmentIds = descendants)
        }

        if (principal.roles.contains(AppRole.PERSON)) {
            val employee =
                employeeRepository.findByUserId(principal.userId)
                    ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")

            val employeeId =
                employee.id
                    ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee profile is incomplete")

            return VaccinationReadScope(employeeId = employeeId)
        }

        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions")
    }

    /**
     * Собирает идентификаторы подразделения и всех его потомков.
     */
    private fun collectDescendants(rootId: UUID): Set<UUID> {
        val allDepartments = departmentRepository.findAll()
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
}

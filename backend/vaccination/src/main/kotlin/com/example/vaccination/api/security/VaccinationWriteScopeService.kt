package com.example.vaccination.api.security

import com.example.auth.AppRole
import com.example.auth.AuthenticatedPrincipal
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeRepository
import com.example.vaccination.document.DocumentRepository
import com.example.vaccination.vaccination.VaccinationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class VaccinationWriteScopeService(
    private val employeeRepository: EmployeeRepository,
    private val departmentRepository: DepartmentRepository,
    private val vaccinationRepository: VaccinationRepository,
    private val documentRepository: DocumentRepository,
) {
    fun assertVaccinationCreateAllowed(
        principal: AuthenticatedPrincipal,
        employeeId: UUID,
    ) {
        if (isAdmin(principal)) {
            return
        }
        val allowedDepartmentIds = medicalAllowedDepartments(principal)
        assertEmployeeInScope(employeeId, allowedDepartmentIds)
    }

    fun assertVaccinationUpdateAllowed(
        principal: AuthenticatedPrincipal,
        vaccinationId: UUID,
        requestedEmployeeId: UUID,
    ) {
        if (isAdmin(principal)) {
            return
        }
        val allowedDepartmentIds = medicalAllowedDepartments(principal)
        assertExistingVaccinationInScope(vaccinationId, allowedDepartmentIds)
        assertEmployeeInScope(requestedEmployeeId, allowedDepartmentIds)
    }

    fun assertVaccinationDeleteAllowed(
        principal: AuthenticatedPrincipal,
        vaccinationId: UUID,
    ) {
        if (isAdmin(principal)) {
            return
        }
        val allowedDepartmentIds = medicalAllowedDepartments(principal)
        assertExistingVaccinationInScope(vaccinationId, allowedDepartmentIds)
    }

    fun assertDocumentCreateAllowed(
        principal: AuthenticatedPrincipal,
        vaccinationId: UUID,
    ) {
        if (isAdmin(principal)) {
            return
        }
        val allowedDepartmentIds = medicalAllowedDepartments(principal)
        assertVaccinationIdInScope(vaccinationId, allowedDepartmentIds)
    }

    fun assertDocumentUpdateAllowed(
        principal: AuthenticatedPrincipal,
        documentId: UUID,
        requestedVaccinationId: UUID,
    ) {
        if (isAdmin(principal)) {
            return
        }
        val allowedDepartmentIds = medicalAllowedDepartments(principal)
        assertExistingDocumentInScope(documentId, allowedDepartmentIds)
        assertVaccinationIdInScope(requestedVaccinationId, allowedDepartmentIds)
    }

    fun assertDocumentDeleteAllowed(
        principal: AuthenticatedPrincipal,
        documentId: UUID,
    ) {
        if (isAdmin(principal)) {
            return
        }
        val allowedDepartmentIds = medicalAllowedDepartments(principal)
        assertExistingDocumentInScope(documentId, allowedDepartmentIds)
    }

    private fun medicalAllowedDepartments(principal: AuthenticatedPrincipal): Set<UUID> {
        if (!principal.roles.contains(AppRole.MEDICAL)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Write access is outside role scope")
        }

        val employee =
            employeeRepository.findByUserId(principal.userId)
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "No employee profile for current user")

        val rootDepartmentId =
            employee.departmentId
                ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee has no department")

        return collectDescendants(rootDepartmentId, departmentRepository.findAll())
    }

    private fun assertExistingVaccinationInScope(
        vaccinationId: UUID,
        allowedDepartmentIds: Set<UUID>,
    ) {
        val existing = vaccinationRepository.findById(vaccinationId).orElse(null) ?: return
        val employeeId = existing.employeeId ?: return
        assertEmployeeInScope(employeeId, allowedDepartmentIds)
    }

    private fun assertExistingDocumentInScope(
        documentId: UUID,
        allowedDepartmentIds: Set<UUID>,
    ) {
        val existing = documentRepository.findById(documentId).orElse(null) ?: return
        val vaccinationId = existing.vaccinationId ?: return
        assertVaccinationIdInScope(vaccinationId, allowedDepartmentIds)
    }

    private fun assertVaccinationIdInScope(
        vaccinationId: UUID,
        allowedDepartmentIds: Set<UUID>,
    ) {
        val vaccination = vaccinationRepository.findById(vaccinationId).orElse(null) ?: return
        val employeeId = vaccination.employeeId ?: return
        assertEmployeeInScope(employeeId, allowedDepartmentIds)
    }

    private fun assertEmployeeInScope(
        employeeId: UUID,
        allowedDepartmentIds: Set<UUID>,
    ) {
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return
        val departmentId = employee.departmentId
        if (departmentId == null || !allowedDepartmentIds.contains(departmentId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Write access is outside department scope")
        }
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

    private fun isAdmin(principal: AuthenticatedPrincipal): Boolean = principal.roles.contains(AppRole.ADMIN)
}

package com.example.employee.department

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
) {
    @Transactional(readOnly = true)
    fun list(): List<DepartmentEntity> = departmentRepository.findAll()

    @Transactional(readOnly = true)
    fun get(id: UUID): DepartmentEntity = findDepartment(id)

    @Transactional
    fun create(command: CreateDepartmentCommand): DepartmentEntity {
        requireParentExists(command.parentId)

        val entity =
            DepartmentEntity(
                name = command.name.trim(),
                parentId = command.parentId,
            )
        return departmentRepository.saveAndFlush(entity)
    }

    @Transactional
    fun update(
        id: UUID,
        command: UpdateDepartmentCommand,
    ): DepartmentEntity {
        val department = findDepartment(id)
        requireParentExists(command.parentId)
        requireNoCycle(id, command.parentId)

        department.name = command.name.trim()
        department.parentId = command.parentId
        return departmentRepository.saveAndFlush(department)
    }

    @Transactional
    fun delete(id: UUID) {
        findDepartment(id)
        if (departmentRepository.existsByParentId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Department has child departments")
        }
        if (employeeRepository.existsByDepartmentId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Department has employees")
        }
        departmentRepository.deleteById(id)
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
}

data class CreateDepartmentCommand(
    val name: String,
    val parentId: UUID?,
)

data class UpdateDepartmentCommand(
    val name: String,
    val parentId: UUID?,
)

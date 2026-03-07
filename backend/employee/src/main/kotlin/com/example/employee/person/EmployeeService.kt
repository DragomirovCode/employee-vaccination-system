package com.example.employee.person

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
) {
    @Transactional(readOnly = true)
    fun list(): List<EmployeeEntity> = employeeRepository.findAll()

    @Transactional(readOnly = true)
    fun get(id: UUID): EmployeeEntity = findEmployee(id)

    @Transactional
    fun create(command: CreateEmployeeCommand): EmployeeEntity {
        validateDepartment(command.departmentId)
        validateUserUniqueness(command.userId, null)

        return try {
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
        } catch (ex: DataIntegrityViolationException) {
            throw toConflict(ex)
        }
    }

    @Transactional
    fun update(
        id: UUID,
        command: UpdateEmployeeCommand,
    ): EmployeeEntity {
        val employee = findEmployee(id)
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
            employeeRepository.saveAndFlush(employee)
        } catch (ex: DataIntegrityViolationException) {
            throw toConflict(ex)
        }
    }

    @Transactional
    fun delete(id: UUID) {
        findEmployee(id)
        employeeRepository.deleteById(id)
    }

    private fun validateDepartment(departmentId: UUID) {
        if (!departmentRepository.existsById(departmentId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "departmentId does not exist")
        }
    }

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

    private fun findEmployee(id: UUID): EmployeeEntity =
        employeeRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }

    private fun toConflict(ex: DataIntegrityViolationException): ResponseStatusException {
        val message = ex.rootCause?.message ?: ex.message ?: "Data integrity violation"
        return ResponseStatusException(HttpStatus.CONFLICT, message)
    }
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

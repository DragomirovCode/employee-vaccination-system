package com.example.employee.person

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeRepository : JpaRepository<EmployeeEntity, UUID> {
    fun findByUserId(userId: UUID): EmployeeEntity?

    fun findAllByDepartmentId(departmentId: UUID): List<EmployeeEntity>

    fun existsByDepartmentId(departmentId: UUID): Boolean
}

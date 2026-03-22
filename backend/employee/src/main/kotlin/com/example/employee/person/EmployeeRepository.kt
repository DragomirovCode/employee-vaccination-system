package com.example.employee.person

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeRepository : JpaRepository<EmployeeEntity, UUID> {
    /**
     * Ищет сотрудника, связанного с указанной учетной записью пользователя.
     */
    fun findByUserId(userId: UUID): EmployeeEntity?

    /**
     * Возвращает сотрудников конкретного подразделения.
     */
    fun findAllByDepartmentId(departmentId: UUID): List<EmployeeEntity>

    /**
     * Возвращает сотрудников набора подразделений.
     */
    fun findAllByDepartmentIdIn(departmentIds: Collection<UUID>): List<EmployeeEntity>

    /**
     * Проверяет, есть ли сотрудники в указанном подразделении.
     */
    fun existsByDepartmentId(departmentId: UUID): Boolean
}

package com.example.employee.department

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DepartmentRepository : JpaRepository<DepartmentEntity, UUID>
{
    fun existsByParentId(parentId: UUID): Boolean
}

package com.example.reporting.access

import java.util.UUID

/**
 * Область данных, доступных пользователю при построении отчетов.
 */
data class ReportingAccessScope(
    /** Идентификатор сотрудника, если доступ ограничен одной персоной. */
    val employeeId: UUID? = null,
    /** Набор доступных подразделений, если доступ ограничен организационной структурой. */
    val departmentIds: Set<UUID>? = null,
)

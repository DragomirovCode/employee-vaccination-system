package com.example.vaccination.api.security

import java.util.UUID

/**
 * Область данных, доступных пользователю при чтении записей вакцинации.
 */
data class VaccinationReadScope(
    /** Идентификатор сотрудника при персональном доступе. */
    val employeeId: UUID? = null,
    /** Набор доступных подразделений при организационном доступе. */
    val departmentIds: Set<UUID>? = null,
)

package com.example.vaccination.api.security

import java.util.UUID

data class VaccinationReadScope(
    val employeeId: UUID? = null,
    val departmentIds: Set<UUID>? = null,
)

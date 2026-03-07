package com.example.reporting.access

import java.util.UUID

data class ReportingAccessScope(
    val employeeId: UUID? = null,
    val departmentIds: Set<UUID>? = null,
)

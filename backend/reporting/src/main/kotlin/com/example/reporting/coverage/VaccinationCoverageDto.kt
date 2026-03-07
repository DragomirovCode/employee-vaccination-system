package com.example.reporting.coverage

import java.util.UUID

data class VaccinationCoverageItem(
    val departmentId: UUID,
    val departmentName: String,
    val employeesTotal: Long,
    val employeesCovered: Long,
    val coveragePercent: Double,
)

data class DepartmentEmployeesTotalRow(
    val departmentId: UUID,
    val departmentName: String,
    val employeesTotal: Long,
)

data class DepartmentEmployeesCoveredRow(
    val departmentId: UUID,
    val employeesCovered: Long,
)

data class VaccinationCoverageByVaccineItem(
    val vaccineId: UUID,
    val vaccineName: String,
    val employeesTotal: Long,
    val employeesCovered: Long,
    val coveragePercent: Double,
)

data class VaccineEmployeesCoveredRow(
    val vaccineId: UUID,
    val vaccineName: String,
    val employeesCovered: Long,
)

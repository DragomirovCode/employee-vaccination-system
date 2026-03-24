package com.example.reporting.coverage

import java.time.LocalDate
import java.util.UUID

/**
 * Строка агрегированного отчета по охвату вакцинацией в разрезе подразделения.
 */
data class VaccinationCoverageItem(
    val departmentId: UUID,
    val departmentName: String,
    val employeesTotal: Long,
    val employeesCovered: Long,
    val coveragePercent: Double,
)

/**
 * Внутренняя проекция с общей численностью сотрудников подразделения.
 */
data class DepartmentEmployeesTotalRow(
    val departmentId: UUID,
    val departmentName: String,
    val employeesTotal: Long,
)

/**
 * Внутренняя проекция с количеством охваченных сотрудников по подразделению.
 */
data class DepartmentEmployeesCoveredRow(
    val departmentId: UUID,
    val employeesCovered: Long,
)

/**
 * Строка отчета по охвату вакцинацией в разрезе сотрудника.
 * Используется для drill-down экрана и экспорта.
 */
data class VaccinationCoverageByEmployeeItem(
    val employeeId: UUID,
    val fullName: String,
    val departmentId: UUID,
    val departmentName: String,
    val isCovered: Boolean,
    val status: EmployeeVaccinationCoverageStatus,
    val revaccinationDate: LocalDate?,
)

/**
 * Внутренняя проекция сотрудника для drill-down отчета по охвату.
 */
data class EmployeeCoverageRow(
    val employeeId: UUID,
    val fullName: String,
    val departmentId: UUID,
    val departmentName: String,
)

/**
 * Внутренняя проекция сотрудника с актуальной вакцинацией.
 */
data class CoveredEmployeeRow(
    val employeeId: UUID,
    val revaccinationDate: LocalDate,
)

/**
 * Статус охвата вакцинацией на уровне сотрудника.
 */
enum class EmployeeVaccinationCoverageStatus {
    CURRENT,
    DUE_SOON,
    MISSING,
}

/**
 * Строка агрегированного отчета по охвату вакцинацией в разрезе вакцины.
 */
data class VaccinationCoverageByVaccineItem(
    val vaccineId: UUID,
    val vaccineName: String,
    val employeesTotal: Long,
    val employeesCovered: Long,
    val coveragePercent: Double,
)

/**
 * Внутренняя проекция с количеством охваченных сотрудников по вакцине.
 */
data class VaccineEmployeesCoveredRow(
    val vaccineId: UUID,
    val vaccineName: String,
    val employeesCovered: Long,
)

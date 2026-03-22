package com.example.reporting.coverage

import java.util.UUID

/**
 * Элемент отчета по охвату вакцинацией в разрезе подразделений.
 */
data class VaccinationCoverageItem(
    /** Идентификатор подразделения. */
    val departmentId: UUID,
    /** Название подразделения. */
    val departmentName: String,
    /** Общее количество сотрудников подразделения. */
    val employeesTotal: Long,
    /** Количество сотрудников с актуальной вакцинацией за период. */
    val employeesCovered: Long,
    /** Процент охвата вакцинацией. */
    val coveragePercent: Double,
)

/**
 * Внутренняя строка с общим количеством сотрудников подразделения.
 */
data class DepartmentEmployeesTotalRow(
    /** Идентификатор подразделения. */
    val departmentId: UUID,
    /** Название подразделения. */
    val departmentName: String,
    /** Общее количество сотрудников подразделения. */
    val employeesTotal: Long,
)

/**
 * Внутренняя строка с количеством охваченных сотрудников подразделения.
 */
data class DepartmentEmployeesCoveredRow(
    /** Идентификатор подразделения. */
    val departmentId: UUID,
    /** Количество сотрудников с актуальной вакцинацией. */
    val employeesCovered: Long,
)

/**
 * Элемент отчета по охвату вакцинацией в разрезе вакцин.
 */
data class VaccinationCoverageByVaccineItem(
    /** Идентификатор вакцины. */
    val vaccineId: UUID,
    /** Название вакцины. */
    val vaccineName: String,
    /** Общее количество сотрудников в доступной области отчета. */
    val employeesTotal: Long,
    /** Количество сотрудников, охваченных данной вакциной. */
    val employeesCovered: Long,
    /** Процент охвата по вакцине. */
    val coveragePercent: Double,
)

/**
 * Внутренняя строка с количеством охваченных сотрудников по вакцине.
 */
data class VaccineEmployeesCoveredRow(
    /** Идентификатор вакцины. */
    val vaccineId: UUID,
    /** Название вакцины. */
    val vaccineName: String,
    /** Количество сотрудников, охваченных данной вакциной. */
    val employeesCovered: Long,
)

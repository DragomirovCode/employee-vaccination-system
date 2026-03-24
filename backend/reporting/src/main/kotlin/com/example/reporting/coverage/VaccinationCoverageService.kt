package com.example.reporting.coverage

import com.example.reporting.access.ReportingAccessScope
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Строит отчеты по охвату вакцинацией для подразделений, сотрудников и вакцин.
 */
@Service
class VaccinationCoverageService(
    private val queryRepository: VaccinationCoverageQueryRepository,
) {
    private companion object {
        const val DUE_SOON_DAYS = 30L
    }

    fun getCoverageByDepartment(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        scope: ReportingAccessScope,
    ): List<VaccinationCoverageItem> {
        require(!dateFrom.isAfter(dateTo)) { "dateFrom must be <= dateTo" }

        val totals =
            queryRepository.findDepartmentTotals(
                departmentIds = scope.departmentIds,
                employeeId = scope.employeeId,
            )
        if (totals.isEmpty()) {
            return emptyList()
        }

        val coveredByDepartment =
            queryRepository
                .findDepartmentCovered(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    departmentIds = scope.departmentIds,
                    employeeId = scope.employeeId,
                    today = LocalDate.now(),
                ).associateBy({ it.departmentId }, { it.employeesCovered })

        return totals.map { total ->
            val covered = coveredByDepartment[total.departmentId] ?: 0L
            VaccinationCoverageItem(
                departmentId = total.departmentId,
                departmentName = total.departmentName,
                employeesTotal = total.employeesTotal,
                employeesCovered = covered,
                coveragePercent = calculateCoverage(covered, total.employeesTotal),
            )
        }
    }

    fun getCoverageByEmployee(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        scope: ReportingAccessScope,
    ): List<VaccinationCoverageByEmployeeItem> {
        require(!dateFrom.isAfter(dateTo)) { "dateFrom must be <= dateTo" }

        val employees =
            queryRepository.findEmployeesInScope(
                departmentIds = scope.departmentIds,
                employeeId = scope.employeeId,
            )
        if (employees.isEmpty()) {
            return emptyList()
        }

        val coveredEmployeeIds =
            queryRepository
                .findCoveredEmployeeIds(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    departmentIds = scope.departmentIds,
                    employeeId = scope.employeeId,
                    today = LocalDate.now(),
                ).associateBy({ it.employeeId }, { it.revaccinationDate })

        val dueSoonDate = LocalDate.now().plusDays(DUE_SOON_DAYS)

        return employees.map { employee ->
            val revaccinationDate = coveredEmployeeIds[employee.employeeId]
            val status =
                when {
                    revaccinationDate == null -> EmployeeVaccinationCoverageStatus.MISSING
                    !revaccinationDate.isAfter(dueSoonDate) -> EmployeeVaccinationCoverageStatus.DUE_SOON
                    else -> EmployeeVaccinationCoverageStatus.CURRENT
                }

            VaccinationCoverageByEmployeeItem(
                employeeId = employee.employeeId,
                fullName = employee.fullName,
                departmentId = employee.departmentId,
                departmentName = employee.departmentName,
                isCovered = revaccinationDate != null,
                status = status,
                revaccinationDate = revaccinationDate,
            )
        }
    }

    fun getCoverageByVaccine(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        scope: ReportingAccessScope,
    ): List<VaccinationCoverageByVaccineItem> {
        require(!dateFrom.isAfter(dateTo)) { "dateFrom must be <= dateTo" }

        val employeesTotal =
            queryRepository.countEmployeesInScope(
                departmentIds = scope.departmentIds,
                employeeId = scope.employeeId,
            )
        if (employeesTotal == 0L) {
            return emptyList()
        }

        return queryRepository
            .findVaccineCovered(
                dateFrom = dateFrom,
                dateTo = dateTo,
                departmentIds = scope.departmentIds,
                employeeId = scope.employeeId,
                today = LocalDate.now(),
            ).map { covered ->
                VaccinationCoverageByVaccineItem(
                    vaccineId = covered.vaccineId,
                    vaccineName = covered.vaccineName,
                    employeesTotal = employeesTotal,
                    employeesCovered = covered.employeesCovered,
                    coveragePercent = calculateCoverage(covered.employeesCovered, employeesTotal),
                )
            }
    }

    private fun calculateCoverage(
        covered: Long,
        total: Long,
    ): Double {
        if (total == 0L) {
            return 0.0
        }

        return BigDecimal(covered * 100.0 / total)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}

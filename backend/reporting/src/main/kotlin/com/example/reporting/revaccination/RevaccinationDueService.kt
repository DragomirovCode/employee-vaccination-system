package com.example.reporting.revaccination

import com.example.reporting.access.ReportingAccessScope
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class RevaccinationDueService(
    private val queryRepository: RevaccinationDueQueryRepository,
) {
    /**
     * Возвращает постраничный список сотрудников, которым требуется ревакцинация в ближайшие N дней.
     */
    fun getDueInDays(
        days: Int,
        scope: ReportingAccessScope,
        pageable: Pageable,
    ): Page<RevaccinationDueItem> {
        require(days >= 0) { "days must be >= 0" }

        val fromDate = LocalDate.now()
        val toDate = fromDate.plusDays(days.toLong())

        return queryRepository
            .findDueInPeriod(
                fromDate = fromDate,
                toDate = toDate,
                departmentIds = scope.departmentIds,
                employeeId = scope.employeeId,
                pageable = pageable,
            ).map { row ->
                RevaccinationDueItem(
                    employeeId = row.employeeId,
                    fullName = buildFullName(row.lastName, row.firstName, row.middleName),
                    departmentId = row.departmentId,
                    departmentName = row.departmentName,
                    vaccineName = row.vaccineName,
                    lastVaccinationDate = row.vaccinationDate,
                    revaccinationDate = row.revaccinationDate,
                    daysLeft = ChronoUnit.DAYS.between(fromDate, row.revaccinationDate),
                )
            }
    }

    /**
     * Возвращает полный список сотрудников для экспорта отчета по ревакцинации.
     */
    fun getDueInDaysForExport(
        days: Int,
        scope: ReportingAccessScope,
    ): List<RevaccinationDueItem> {
        require(days >= 0) { "days must be >= 0" }
        val fromDate = LocalDate.now()
        val toDate = fromDate.plusDays(days.toLong())

        return queryRepository
            .findDueInPeriodForExport(
                fromDate = fromDate,
                toDate = toDate,
                departmentIds = scope.departmentIds,
                employeeId = scope.employeeId,
            ).map { row ->
                RevaccinationDueItem(
                    employeeId = row.employeeId,
                    fullName = buildFullName(row.lastName, row.firstName, row.middleName),
                    departmentId = row.departmentId,
                    departmentName = row.departmentName,
                    vaccineName = row.vaccineName,
                    lastVaccinationDate = row.vaccinationDate,
                    revaccinationDate = row.revaccinationDate,
                    daysLeft = ChronoUnit.DAYS.between(fromDate, row.revaccinationDate),
                )
            }
    }

    /**
     * Собирает полное имя сотрудника из отдельных частей.
     */
    private fun buildFullName(
        lastName: String,
        firstName: String,
        middleName: String?,
    ): String =
        listOfNotNull(lastName, firstName, middleName?.takeIf { it.isNotBlank() })
            .joinToString(" ")
}

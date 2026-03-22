package com.example.reporting.export

import com.example.reporting.coverage.VaccinationCoverageByVaccineItem
import com.example.reporting.coverage.VaccinationCoverageItem
import com.example.reporting.revaccination.RevaccinationDueItem
import java.util.Locale

/**
 * Табличное представление отчета, готовое к экспорту.
 */
data class ReportTableView(
    /** Заголовки столбцов. */
    val headers: List<String>,
    /** Строки таблицы. */
    val rows: List<List<Any?>>,
)

object ReportExportViewModels {
    /**
     * Преобразует отчет по ревакцинации в табличный вид для экспорта.
     */
    fun revaccinationDue(
        items: List<RevaccinationDueItem>,
        locale: Locale,
    ): ReportTableView =
        ReportTableView(
            headers =
                localizedHeaders(
                    locale,
                    "Employee",
                    "Department",
                    "Vaccine",
                    "Last vaccination date",
                    "Revaccination date",
                    "Days left",
                ),
            rows =
                items.map {
                    listOf(
                        it.fullName,
                        it.departmentName ?: it.departmentId,
                        it.vaccineName,
                        it.lastVaccinationDate,
                        it.revaccinationDate,
                        it.daysLeft,
                    )
                },
        )

    /**
     * Преобразует отчет по охвату вакцинацией по подразделениям в табличный вид.
     */
    fun coverageByDepartment(
        items: List<VaccinationCoverageItem>,
        locale: Locale,
    ): ReportTableView =
        ReportTableView(
            headers = localizedHeaders(locale, "Department", "Employees total", "Employees covered", "Coverage percent"),
            rows =
                items.map {
                    listOf(
                        it.departmentName,
                        it.employeesTotal,
                        it.employeesCovered,
                        it.coveragePercent,
                    )
                },
        )

    /**
     * Преобразует отчет по охвату вакцинацией по вакцинам в табличный вид.
     */
    fun coverageByVaccine(
        items: List<VaccinationCoverageByVaccineItem>,
        locale: Locale,
    ): ReportTableView =
        ReportTableView(
            headers = localizedHeaders(locale, "Vaccine", "Employees total", "Employees covered", "Coverage percent"),
            rows =
                items.map {
                    listOf(
                        it.vaccineName,
                        it.employeesTotal,
                        it.employeesCovered,
                        it.coveragePercent,
                    )
                },
        )

    /**
     * Возвращает локализованные заголовки таблицы.
     */
    private fun localizedHeaders(
        locale: Locale,
        vararg englishHeaders: String,
    ): List<String> =
        if (locale.language.lowercase() == "ru") {
            englishHeaders.map(::toRussianHeader)
        } else {
            englishHeaders.toList()
        }

    /**
     * Переводит известные английские заголовки таблиц на русский язык.
     */
    private fun toRussianHeader(header: String): String =
        when (header) {
            "Employee" -> "Сотрудник"
            "Department" -> "Подразделение"
            "Vaccine" -> "Вакцина"
            "Last vaccination date" -> "Дата последней вакцинации"
            "Revaccination date" -> "Дата ревакцинации"
            "Days left" -> "Осталось дней"
            "Employees total" -> "Всего сотрудников"
            "Employees covered" -> "Привито сотрудников"
            "Coverage percent" -> "Процент охвата"
            else -> header
        }
}

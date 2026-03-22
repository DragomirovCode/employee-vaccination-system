package com.example.reporting.revaccination

import java.time.LocalDate
import java.util.UUID

/**
 * Элемент отчета о сотрудниках, которым требуется ревакцинация.
 */
data class RevaccinationDueItem(
    /** Идентификатор сотрудника. */
    val employeeId: UUID,
    /** Полное имя сотрудника. */
    val fullName: String,
    /** Идентификатор подразделения сотрудника. */
    val departmentId: UUID,
    /** Название подразделения сотрудника. */
    val departmentName: String? = null,
    /** Название вакцины. */
    val vaccineName: String,
    /** Дата последней вакцинации. */
    val lastVaccinationDate: LocalDate,
    /** Дата, к которой требуется ревакцинация. */
    val revaccinationDate: LocalDate,
    /** Количество дней до ревакцинации. */
    val daysLeft: Long,
)

/**
 * Внутренняя строка выборки для отчета о предстоящей ревакцинации.
 */
data class RevaccinationDueRow(
    /** Идентификатор сотрудника. */
    val employeeId: UUID,
    /** Имя сотрудника. */
    val firstName: String,
    /** Фамилия сотрудника. */
    val lastName: String,
    /** Отчество сотрудника. */
    val middleName: String?,
    /** Идентификатор подразделения сотрудника. */
    val departmentId: UUID,
    /** Название подразделения сотрудника. */
    val departmentName: String?,
    /** Название вакцины. */
    val vaccineName: String,
    /** Дата последней вакцинации. */
    val vaccinationDate: LocalDate,
    /** Дата ревакцинации. */
    val revaccinationDate: LocalDate,
)

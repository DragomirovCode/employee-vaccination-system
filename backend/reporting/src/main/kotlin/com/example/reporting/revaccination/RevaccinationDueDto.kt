package com.example.reporting.revaccination

import java.time.LocalDate
import java.util.UUID

data class RevaccinationDueItem(
    val employeeId: UUID,
    val fullName: String,
    val departmentId: UUID,
    val departmentName: String? = null,
    val vaccineName: String,
    val lastVaccinationDate: LocalDate,
    val revaccinationDate: LocalDate,
    val daysLeft: Long,
)

data class RevaccinationDueRow(
    val employeeId: UUID,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val departmentId: UUID,
    val departmentName: String?,
    val vaccineName: String,
    val vaccinationDate: LocalDate,
    val revaccinationDate: LocalDate,
)

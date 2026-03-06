package com.example.reporting.coverage

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class VaccinationCoverageQueryRepository(
    private val entityManager: EntityManager,
) {
    fun findDepartmentTotals(departmentId: UUID?): List<DepartmentEmployeesTotalRow> {
        val whereClause = if (departmentId != null) "WHERE d.id = :departmentId" else ""

        val query =
            entityManager.createQuery(
                """
                SELECT new com.example.reporting.coverage.DepartmentEmployeesTotalRow(
                    d.id, d.name, COUNT(e.id)
                )
                FROM EmployeeEntity e
                JOIN DepartmentEntity d ON d.id = e.departmentId
                $whereClause
                GROUP BY d.id, d.name
                ORDER BY d.name ASC
                """.trimIndent(),
                DepartmentEmployeesTotalRow::class.java,
            )

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId)
        }

        return query.resultList
    }

    fun findDepartmentCovered(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        departmentId: UUID?,
        today: LocalDate,
    ): List<DepartmentEmployeesCoveredRow> {
        val whereClause =
            buildString {
                append("v.vaccinationDate BETWEEN :dateFrom AND :dateTo ")
                append("AND v.revaccinationDate IS NOT NULL ")
                append("AND v.revaccinationDate >= :today ")
                if (departmentId != null) {
                    append("AND e.departmentId = :departmentId ")
                }
            }

        val query =
            entityManager
                .createQuery(
                    """
                    SELECT new com.example.reporting.coverage.DepartmentEmployeesCoveredRow(
                        e.departmentId, COUNT(DISTINCT e.id)
                    )
                    FROM VaccinationEntity v
                    JOIN EmployeeEntity e ON e.id = v.employeeId
                    WHERE $whereClause
                    GROUP BY e.departmentId
                    """.trimIndent(),
                    DepartmentEmployeesCoveredRow::class.java,
                ).setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo)
                .setParameter("today", today)

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId)
        }

        return query.resultList
    }
}

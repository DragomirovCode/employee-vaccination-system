package com.example.reporting.coverage

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class VaccinationCoverageQueryRepository(
    private val entityManager: EntityManager,
) {
    fun countEmployeesInScope(
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
    ): Long {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return 0L
        }

        val whereClause =
            buildString {
                val predicates = mutableListOf<String>()
                if (departmentIds != null) {
                    predicates.add("e.departmentId IN :departmentIds")
                }
                if (employeeId != null) {
                    predicates.add("e.id = :employeeId")
                }
                if (predicates.isNotEmpty()) {
                    append("WHERE ${predicates.joinToString(" AND ")}")
                }
            }

        val query =
            entityManager.createQuery(
                """
                SELECT COUNT(e.id)
                FROM EmployeeEntity e
                $whereClause
                """.trimIndent(),
                java.lang.Long::class.java,
            )

        bindScopeParameters(query, departmentIds, employeeId)
        return query.singleResult.toLong()
    }

    fun findDepartmentTotals(
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
    ): List<DepartmentEmployeesTotalRow> {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return emptyList()
        }

        val whereClause =
            buildString {
                val predicates = mutableListOf<String>()
                if (departmentIds != null) {
                    predicates.add("d.id IN :departmentIds")
                }
                if (employeeId != null) {
                    predicates.add("e.id = :employeeId")
                }
                if (predicates.isNotEmpty()) {
                    append("WHERE ${predicates.joinToString(" AND ")}")
                }
            }

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

        bindScopeParameters(query, departmentIds, employeeId)

        return query.resultList
    }

    fun findVaccineCovered(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
        today: LocalDate,
    ): List<VaccineEmployeesCoveredRow> {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return emptyList()
        }

        val whereClause =
            buildString {
                append("v.vaccinationDate BETWEEN :dateFrom AND :dateTo ")
                append("AND v.revaccinationDate IS NOT NULL ")
                append("AND v.revaccinationDate >= :today ")
                if (departmentIds != null) {
                    append("AND e.departmentId IN :departmentIds ")
                }
                if (employeeId != null) {
                    append("AND e.id = :employeeId ")
                }
            }

        val query =
            entityManager
                .createQuery(
                    """
                    SELECT new com.example.reporting.coverage.VaccineEmployeesCoveredRow(
                        vac.id, vac.name, COUNT(DISTINCT e.id)
                    )
                    FROM VaccinationEntity v
                    JOIN EmployeeEntity e ON e.id = v.employeeId
                    JOIN VaccineEntity vac ON vac.id = v.vaccineId
                    WHERE $whereClause
                    GROUP BY vac.id, vac.name
                    ORDER BY vac.name ASC
                    """.trimIndent(),
                    VaccineEmployeesCoveredRow::class.java,
                ).setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo)
                .setParameter("today", today)

        bindScopeParameters(query, departmentIds, employeeId)

        return query.resultList
    }

    fun findDepartmentCovered(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
        today: LocalDate,
    ): List<DepartmentEmployeesCoveredRow> {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return emptyList()
        }

        val whereClause =
            buildString {
                append("v.vaccinationDate BETWEEN :dateFrom AND :dateTo ")
                append("AND v.revaccinationDate IS NOT NULL ")
                append("AND v.revaccinationDate >= :today ")
                if (departmentIds != null) {
                    append("AND e.departmentId IN :departmentIds ")
                }
                if (employeeId != null) {
                    append("AND e.id = :employeeId ")
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

        bindScopeParameters(query, departmentIds, employeeId)

        return query.resultList
    }

    private fun bindScopeParameters(
        query: jakarta.persistence.TypedQuery<*>,
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
    ) {
        if (departmentIds != null) {
            query.setParameter("departmentIds", departmentIds)
        }
        if (employeeId != null) {
            query.setParameter("employeeId", employeeId)
        }
    }
}

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

        val whereClause = buildEmployeeScopeClause("e", departmentIds, employeeId)
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

    fun findEmployeesInScope(
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
    ): List<EmployeeCoverageRow> {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return emptyList()
        }

        val whereClause = buildEmployeeScopeClause("e", departmentIds, employeeId)
        val query =
            entityManager.createQuery(
                """
                SELECT new com.example.reporting.coverage.EmployeeCoverageRow(
                    e.id,
                    CASE
                        WHEN e.middleName IS NULL OR e.middleName = ''
                            THEN CONCAT(CONCAT(e.lastName, ' '), e.firstName)
                        ELSE CONCAT(CONCAT(CONCAT(e.lastName, ' '), e.firstName), CONCAT(' ', e.middleName))
                    END,
                    d.id,
                    d.name
                )
                FROM EmployeeEntity e
                JOIN DepartmentEntity d ON d.id = e.departmentId
                $whereClause
                ORDER BY d.name ASC, e.lastName ASC, e.firstName ASC, e.middleName ASC
                """.trimIndent(),
                EmployeeCoverageRow::class.java,
            )

        bindScopeParameters(query, departmentIds, employeeId)
        return query.resultList
    }

    fun findCoveredEmployeeIds(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
        today: LocalDate,
    ): List<CoveredEmployeeRow> {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return emptyList()
        }

        val whereClause = buildVaccinationScopeClause(departmentIds, employeeId)
        val query =
            entityManager
                .createQuery(
                    """
                    SELECT new com.example.reporting.coverage.CoveredEmployeeRow(
                        e.id, MIN(v.revaccinationDate)
                    )
                    FROM VaccinationEntity v
                    JOIN EmployeeEntity e ON e.id = v.employeeId
                    WHERE v.vaccinationDate BETWEEN :dateFrom AND :dateTo
                      AND v.revaccinationDate IS NOT NULL
                      AND v.revaccinationDate >= :today
                      $whereClause
                    GROUP BY e.id
                    """.trimIndent(),
                    CoveredEmployeeRow::class.java,
                ).setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo)
                .setParameter("today", today)

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

        val whereClause = buildVaccinationScopeClause(departmentIds, employeeId)
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
                    WHERE v.vaccinationDate BETWEEN :dateFrom AND :dateTo
                      AND v.revaccinationDate IS NOT NULL
                      AND v.revaccinationDate >= :today
                      $whereClause
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

        val whereClause = buildVaccinationScopeClause(departmentIds, employeeId)
        val query =
            entityManager
                .createQuery(
                    """
                    SELECT new com.example.reporting.coverage.DepartmentEmployeesCoveredRow(
                        e.departmentId, COUNT(DISTINCT e.id)
                    )
                    FROM VaccinationEntity v
                    JOIN EmployeeEntity e ON e.id = v.employeeId
                    WHERE v.vaccinationDate BETWEEN :dateFrom AND :dateTo
                      AND v.revaccinationDate IS NOT NULL
                      AND v.revaccinationDate >= :today
                      $whereClause
                    GROUP BY e.departmentId
                    """.trimIndent(),
                    DepartmentEmployeesCoveredRow::class.java,
                ).setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo)
                .setParameter("today", today)

        bindScopeParameters(query, departmentIds, employeeId)
        return query.resultList
    }

    private fun buildEmployeeScopeClause(
        employeeAlias: String,
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
    ): String =
        buildString {
            val predicates = mutableListOf<String>()
            if (departmentIds != null) {
                predicates.add("$employeeAlias.departmentId IN :departmentIds")
            }
            if (employeeId != null) {
                predicates.add("$employeeAlias.id = :employeeId")
            }
            if (predicates.isNotEmpty()) {
                append("WHERE ${predicates.joinToString(" AND ")}")
            }
        }

    private fun buildVaccinationScopeClause(
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
    ): String =
        buildString {
            if (departmentIds != null) {
                append("\n  AND e.departmentId IN :departmentIds")
            }
            if (employeeId != null) {
                append("\n  AND e.id = :employeeId")
            }
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

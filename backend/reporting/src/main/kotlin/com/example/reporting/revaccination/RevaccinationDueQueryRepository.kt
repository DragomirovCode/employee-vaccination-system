package com.example.reporting.revaccination

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class RevaccinationDueQueryRepository(
    private val entityManager: EntityManager,
) {
    fun findDueInPeriod(
        fromDate: LocalDate,
        toDate: LocalDate,
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
        pageable: Pageable,
    ): Page<RevaccinationDueRow> {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val whereClause =
            buildString {
                append("v.revaccinationDate IS NOT NULL ")
                append("AND v.revaccinationDate BETWEEN :fromDate AND :toDate ")
                if (departmentIds != null) {
                    append("AND e.departmentId IN :departmentIds ")
                }
                if (employeeId != null) {
                    append("AND e.id = :employeeId ")
                }
            }

        val selectQuery =
            entityManager
                .createQuery(
                    """
                    SELECT new com.example.reporting.revaccination.RevaccinationDueRow(
                        e.id, e.firstName, e.lastName, e.middleName, e.departmentId,
                        vac.name, v.vaccinationDate, v.revaccinationDate
                    )
                    FROM VaccinationEntity v
                    JOIN EmployeeEntity e ON e.id = v.employeeId
                    JOIN VaccineEntity vac ON vac.id = v.vaccineId
                    WHERE $whereClause
                    ORDER BY v.revaccinationDate ASC, e.lastName ASC, e.firstName ASC
                    """.trimIndent(),
                    RevaccinationDueRow::class.java,
                ).setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)

        bindScopeParameters(
            query = selectQuery,
            departmentIds = departmentIds,
            employeeId = employeeId,
        )

        val totalCount =
            entityManager
                .createQuery(
                    """
                    SELECT COUNT(v.id)
                    FROM VaccinationEntity v
                    JOIN EmployeeEntity e ON e.id = v.employeeId
                    WHERE $whereClause
                    """.trimIndent(),
                    java.lang.Long::class.java,
                ).setParameter(
                    "fromDate",
                    fromDate,
                ).setParameter("toDate", toDate)
                .let { query ->
                    bindScopeParameters(
                        query = query,
                        departmentIds = departmentIds,
                        employeeId = employeeId,
                    )
                    query
                }.singleResult
                .toLong()

        val content =
            selectQuery
                .setFirstResult(pageable.offset.toInt())
                .setMaxResults(pageable.pageSize)
                .resultList

        return PageImpl(content, pageable, totalCount)
    }

    fun findDueInPeriodForExport(
        fromDate: LocalDate,
        toDate: LocalDate,
        departmentIds: Set<UUID>?,
        employeeId: UUID?,
    ): List<RevaccinationDueRow> {
        if (departmentIds != null && departmentIds.isEmpty()) {
            return emptyList()
        }

        val whereClause =
            buildString {
                append("v.revaccinationDate IS NOT NULL ")
                append("AND v.revaccinationDate BETWEEN :fromDate AND :toDate ")
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
                    SELECT new com.example.reporting.revaccination.RevaccinationDueRow(
                        e.id, e.firstName, e.lastName, e.middleName, e.departmentId,
                        vac.name, v.vaccinationDate, v.revaccinationDate
                    )
                    FROM VaccinationEntity v
                    JOIN EmployeeEntity e ON e.id = v.employeeId
                    JOIN VaccineEntity vac ON vac.id = v.vaccineId
                    WHERE $whereClause
                    ORDER BY v.revaccinationDate ASC, e.lastName ASC, e.firstName ASC
                    """.trimIndent(),
                    RevaccinationDueRow::class.java,
                ).setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)

        bindScopeParameters(
            query = query,
            departmentIds = departmentIds,
            employeeId = employeeId,
        )

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

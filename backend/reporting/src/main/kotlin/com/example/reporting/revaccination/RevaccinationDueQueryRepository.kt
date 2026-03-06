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
        departmentId: UUID?,
        pageable: Pageable,
    ): Page<RevaccinationDueRow> {
        val whereClause =
            buildString {
                append("v.revaccinationDate IS NOT NULL ")
                append("AND v.revaccinationDate BETWEEN :fromDate AND :toDate ")
                if (departmentId != null) {
                    append("AND e.departmentId = :departmentId ")
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

        if (departmentId != null) {
            selectQuery.setParameter("departmentId", departmentId)
        }

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
                .let {
                    if (departmentId != null) {
                        it.setParameter("departmentId", departmentId)
                    } else {
                        it
                    }
                }.singleResult
                .toLong()

        val content =
            selectQuery
                .setFirstResult(pageable.offset.toInt())
                .setMaxResults(pageable.pageSize)
                .resultList

        return PageImpl(content, pageable, totalCount)
    }
}

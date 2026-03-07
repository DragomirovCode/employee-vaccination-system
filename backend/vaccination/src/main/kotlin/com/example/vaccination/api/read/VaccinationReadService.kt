package com.example.vaccination.api.read

import com.example.auth.AuthenticatedPrincipal
import com.example.employee.person.EmployeeRepository
import com.example.vaccination.api.security.VaccinationReadScope
import com.example.vaccination.api.security.VaccinationReadScopeResolver
import com.example.vaccination.document.DocumentEntity
import com.example.vaccination.document.DocumentRepository
import com.example.vaccination.vaccination.VaccinationEntity
import com.example.vaccination.vaccination.VaccinationRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@Service
class VaccinationReadService(
    private val vaccinationRepository: VaccinationRepository,
    private val documentRepository: DocumentRepository,
    private val employeeRepository: EmployeeRepository,
    private val readScopeResolver: VaccinationReadScopeResolver,
) {
    @Transactional(readOnly = true)
    fun getVaccination(
        principal: AuthenticatedPrincipal,
        id: UUID,
    ): VaccinationEntity {
        val scope = readScopeResolver.resolve(principal)
        val vaccination =
            vaccinationRepository.findById(id).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccination not found")
            }
        assertEmployeeAccessible(scope, vaccination.employeeId)
        return vaccination
    }

    @Transactional(readOnly = true)
    fun listVaccinations(
        principal: AuthenticatedPrincipal,
        filter: VaccinationReadFilter,
        pageable: Pageable,
    ): Page<VaccinationEntity> {
        val scope = readScopeResolver.resolve(principal)
        validateDates(filter.dateFrom, filter.dateTo)

        if (filter.employeeId != null) {
            assertEmployeeAccessible(scope, filter.employeeId)
        }

        val scopedEmployeeIds = resolveScopedEmployeeIds(scope)
        if (scopedEmployeeIds != null && scopedEmployeeIds.isEmpty()) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val specification =
            Specification<VaccinationEntity> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()

                scopedEmployeeIds?.let { predicates += root.get<UUID>("employeeId").`in`(it) }
                filter.employeeId?.let { predicates += cb.equal(root.get<UUID>("employeeId"), it) }
                filter.vaccineId?.let { predicates += cb.equal(root.get<UUID>("vaccineId"), it) }
                filter.dateFrom?.let { predicates += cb.greaterThanOrEqualTo(root.get("vaccinationDate"), it) }
                filter.dateTo?.let { predicates += cb.lessThanOrEqualTo(root.get("vaccinationDate"), it) }

                cb.and(*predicates.toTypedArray())
            }

        return vaccinationRepository.findAll(specification, pageable)
    }

    @Transactional(readOnly = true)
    fun listEmployeeVaccinations(
        principal: AuthenticatedPrincipal,
        employeeId: UUID,
    ): List<VaccinationEntity> {
        val scope = readScopeResolver.resolve(principal)
        assertEmployeeAccessible(scope, employeeId)
        return vaccinationRepository.findAllByEmployeeId(employeeId).sortedByDescending { it.vaccinationDate }
    }

    @Transactional(readOnly = true)
    fun listVaccinationDocuments(
        principal: AuthenticatedPrincipal,
        vaccinationId: UUID,
    ): List<DocumentEntity> {
        val vaccination = getVaccination(principal, vaccinationId)
        val id = vaccination.id ?: return emptyList()
        return documentRepository.findAllByVaccinationIdOrderByUploadedAtDesc(id)
    }

    @Transactional(readOnly = true)
    fun getDocument(
        principal: AuthenticatedPrincipal,
        documentId: UUID,
    ): DocumentEntity {
        val document =
            documentRepository.findById(documentId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")
            }
        val vaccinationId =
            document.vaccinationId
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vaccination not found")
        getVaccination(principal, vaccinationId)
        return document
    }

    private fun resolveScopedEmployeeIds(scope: VaccinationReadScope): Set<UUID>? {
        scope.employeeId?.let { return setOf(it) }
        scope.departmentIds?.let { departmentIds ->
            return employeeRepository.findAllByDepartmentIdIn(departmentIds).mapNotNull { it.id }.toSet()
        }
        return null
    }

    private fun assertEmployeeAccessible(
        scope: VaccinationReadScope,
        employeeId: UUID?,
    ) {
        if (employeeId == null) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee is outside access scope")
        }

        if (scope.employeeId != null) {
            if (scope.employeeId != employeeId) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee is outside access scope")
            }
            return
        }

        if (scope.departmentIds != null) {
            val employee =
                employeeRepository.findById(employeeId).orElse(null)
                    ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee is outside access scope")
            if (employee.departmentId == null || !scope.departmentIds.contains(employee.departmentId)) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Employee is outside access scope")
            }
        }
    }

    private fun validateDates(
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFrom must be <= dateTo")
        }
    }
}

data class VaccinationReadFilter(
    val employeeId: UUID? = null,
    val vaccineId: UUID? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
)

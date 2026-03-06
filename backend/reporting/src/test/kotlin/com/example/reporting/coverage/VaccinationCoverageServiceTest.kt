package com.example.reporting.coverage

import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import com.example.reporting.ReportingTestApplication
import com.example.vaccination.vaccination.VaccinationEntity
import com.example.vaccination.vaccination.VaccinationRepository
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [ReportingTestApplication::class])
class VaccinationCoverageServiceTest {
    @Autowired
    private lateinit var service: VaccinationCoverageService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var vaccinationRepository: VaccinationRepository

    @Test
    fun `returns coverage grouped by department and supports filter`() {
        val seed = seedData()

        val result =
            service.getCoverageByDepartment(
                dateFrom = LocalDate.of(2026, 1, 1),
                dateTo = LocalDate.of(2026, 12, 31),
                departmentId = null,
            )

        assertEquals(2, result.size)

        val a = result.first { it.departmentId == seed.departmentAId }
        assertEquals(3, a.employeesTotal)
        assertEquals(1, a.employeesCovered)
        assertEquals(33.33, a.coveragePercent)

        val b = result.first { it.departmentId == seed.departmentBId }
        assertEquals(2, b.employeesTotal)
        assertEquals(1, b.employeesCovered)
        assertEquals(50.0, b.coveragePercent)

        val filtered =
            service.getCoverageByDepartment(
                dateFrom = LocalDate.of(2026, 1, 1),
                dateTo = LocalDate.of(2026, 12, 31),
                departmentId = seed.departmentAId,
            )
        assertEquals(1, filtered.size)
        assertEquals(seed.departmentAId, filtered.first().departmentId)
    }

    @Test
    fun `returns empty when selected department has no employees`() {
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Empty Department"))

        val result =
            service.getCoverageByDepartment(
                dateFrom = LocalDate.of(2026, 1, 1),
                dateTo = LocalDate.of(2026, 12, 31),
                departmentId = department.id,
            )

        assertTrue(result.isEmpty())
    }

    private fun seedData(): SeedResult {
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()

        val performer = userRepository.saveAndFlush(UserEntity(email = "coverage-service@example.com", passwordHash = "hash"))
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "CoverageVax", validityDays = 365, dosesRequired = 1))

        val departmentA = departmentRepository.saveAndFlush(DepartmentEntity(name = "A Department"))
        val departmentB = departmentRepository.saveAndFlush(DepartmentEntity(name = "B Department"))

        val a1 = employeeRepository.saveAndFlush(EmployeeEntity(departmentId = departmentA.id, firstName = "A1", lastName = "User"))
        val a2 = employeeRepository.saveAndFlush(EmployeeEntity(departmentId = departmentA.id, firstName = "A2", lastName = "User"))
        val a3 = employeeRepository.saveAndFlush(EmployeeEntity(departmentId = departmentA.id, firstName = "A3", lastName = "User"))
        val b1 = employeeRepository.saveAndFlush(EmployeeEntity(departmentId = departmentB.id, firstName = "B1", lastName = "User"))
        employeeRepository.saveAndFlush(EmployeeEntity(departmentId = departmentB.id, firstName = "B2", lastName = "User"))

        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = a1.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 2, 10),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(30),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = a2.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 3, 10),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().minusDays(1),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = a3.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2025, 12, 10),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(30),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = b1.id,
                vaccineId = vaccine.id,
                performedBy = performer.id,
                vaccinationDate = LocalDate.of(2026, 4, 10),
                doseNumber = 1,
                revaccinationDate = LocalDate.now().plusDays(30),
            ),
        )

        return SeedResult(
            departmentAId = departmentA.id!!,
            departmentBId = departmentB.id!!,
        )
    }
}

private data class SeedResult(
    val departmentAId: UUID,
    val departmentBId: UUID,
)

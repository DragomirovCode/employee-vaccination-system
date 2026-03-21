package com.example.reporting.revaccination

import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import com.example.reporting.ReportingTestApplication
import com.example.reporting.access.ReportingAccessScope
import com.example.vaccination.vaccination.VaccinationEntity
import com.example.vaccination.vaccination.VaccinationRepository
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [ReportingTestApplication::class])
class RevaccinationDueServiceTest {
    @Autowired
    private lateinit var service: RevaccinationDueService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var vaccinationRepository: VaccinationRepository

    @Test
    fun `returns due vaccinations within days and department filter`() {
        val seed = seedData()

        val allDue = service.getDueInDays(10, ReportingAccessScope(), PageRequest.of(0, 20))
        assertEquals(2, allDue.totalElements)
        assertTrue(allDue.content.all { it.daysLeft in 0..10 })

        val onlyPrimaryDepartment =
            service.getDueInDays(
                10,
                ReportingAccessScope(departmentIds = setOf(seed.primaryDepartmentId)),
                PageRequest.of(0, 20),
            )
        assertEquals(1, onlyPrimaryDepartment.totalElements)
        assertEquals(seed.primaryEmployeeId, onlyPrimaryDepartment.content.first().employeeId)
    }

    @Test
    fun `returns empty page when no records in period`() {
        seedData()

        val result = service.getDueInDays(0, ReportingAccessScope(), PageRequest.of(0, 20))

        assertEquals(0, result.totalElements)
        assertTrue(result.content.isEmpty())
    }

    private fun seedData(): SeedResult {
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()

        val performer = userRepository.saveAndFlush(UserEntity(email = "medic-report@example.com", passwordHash = "hash"))

        val primaryDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "Ops"))
        val secondaryDepartment = departmentRepository.saveAndFlush(DepartmentEntity(name = "HR"))

        val primaryEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = primaryDepartment.id,
                    firstName = "Ivan",
                    lastName = "Petrov",
                ),
            )
        val secondaryEmployee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = secondaryDepartment.id,
                    firstName = "Anna",
                    lastName = "Sidorova",
                ),
            )

        val vaccineA = vaccineRepository.saveAndFlush(VaccineEntity(name = "Flu A", validityDays = 365, dosesRequired = 1))
        val vaccineB = vaccineRepository.saveAndFlush(VaccineEntity(name = "Flu B", validityDays = 365, dosesRequired = 1))

        val today = LocalDate.now()

        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = primaryEmployee.id,
                vaccineId = vaccineA.id,
                performedBy = performer.id,
                vaccinationDate = today.minusDays(300),
                doseNumber = 1,
                expirationDate = today.plusDays(30),
                revaccinationDate = today.plusDays(5),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = secondaryEmployee.id,
                vaccineId = vaccineB.id,
                performedBy = performer.id,
                vaccinationDate = today.minusDays(200),
                doseNumber = 1,
                expirationDate = today.plusDays(30),
                revaccinationDate = today.plusDays(2),
            ),
        )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = secondaryEmployee.id,
                vaccineId = vaccineB.id,
                performedBy = performer.id,
                vaccinationDate = today.minusDays(150),
                doseNumber = 1,
                expirationDate = today.plusDays(30),
                revaccinationDate = today.plusDays(40),
            ),
        )

        return SeedResult(
            primaryDepartmentId = primaryDepartment.id!!,
            primaryEmployeeId = primaryEmployee.id!!,
        )
    }
}

private data class SeedResult(
    val primaryDepartmentId: UUID,
    val primaryEmployeeId: UUID,
)

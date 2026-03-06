package com.example.vaccination

import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import com.example.vaccination.vaccination.CreateVaccinationCommand
import com.example.vaccination.vaccination.VaccinationRepository
import com.example.vaccination.vaccination.VaccinationService
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [VaccinationTestApplication::class])
class VaccinationServiceTest {
    @Autowired
    private lateinit var vaccinationService: VaccinationService

    @Autowired
    private lateinit var vaccinationRepository: VaccinationRepository

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Test
    fun `calculates next dose and revaccination dates`() {
        vaccinationRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        userRepository.deleteAll()
        vaccineRepository.deleteAll()

        val user = userRepository.saveAndFlush(UserEntity(email = "medic@example.com", passwordHash = "hash"))
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Ops"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Alex",
                    lastName = "Worker",
                ),
            )
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "Two dose",
                    validityDays = 180,
                    dosesRequired = 2,
                    daysBetween = 30,
                ),
            )

        val created =
            vaccinationService.create(
                CreateVaccinationCommand(
                    employeeId = employee.id!!,
                    vaccineId = vaccine.id!!,
                    performedBy = user.id!!,
                    vaccinationDate = LocalDate.of(2026, 3, 1),
                    doseNumber = 1,
                ),
            )

        assertNotNull(created.id)
        assertEquals(LocalDate.of(2026, 3, 31), created.nextDoseDate)
        assertEquals(LocalDate.of(2026, 8, 28), created.revaccinationDate)

        val completedCourse =
            vaccinationService.create(
                CreateVaccinationCommand(
                    employeeId = employee.id!!,
                    vaccineId = vaccine.id!!,
                    performedBy = user.id!!,
                    vaccinationDate = LocalDate.of(2026, 4, 1),
                    doseNumber = 2,
                ),
            )

        assertEquals(null, completedCourse.nextDoseDate)
        assertEquals(LocalDate.of(2026, 9, 28), completedCourse.revaccinationDate)
    }
}

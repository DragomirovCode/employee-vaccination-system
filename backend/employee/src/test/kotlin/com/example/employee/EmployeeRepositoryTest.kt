package com.example.employee

import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

@SpringBootTest(classes = [EmployeeTestApplication::class])
class EmployeeRepositoryTest {
    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @BeforeEach
    fun setUp() {
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
    }

    @Test
    fun `save employee with nullable user id`() {
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "HR"))

        val saved =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Ivan",
                    lastName = "Petrov",
                ),
            )

        assertNotNull(saved.id)
        assertEquals(7, saved.id!!.version())
        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    @Test
    fun `save and find employee by user id`() {
        val userId = UUID.randomUUID()
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "IT"))

        employeeRepository.saveAndFlush(
            EmployeeEntity(
                userId = userId,
                departmentId = department.id,
                firstName = "Anna",
                lastName = "Sidorova",
            ),
        )

        val found = employeeRepository.findByUserId(userId)

        assertNotNull(found)
        assertEquals(userId, found?.userId)
        assertEquals(department.id, found?.departmentId)
    }

    @Test
    fun `duplicate user id is rejected`() {
        val userId = UUID.randomUUID()
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Finance"))

        employeeRepository.saveAndFlush(
            EmployeeEntity(
                userId = userId,
                departmentId = department.id,
                firstName = "Oleg",
                lastName = "Smirnov",
            ),
        )

        assertThrows<DataIntegrityViolationException> {
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    userId = userId,
                    departmentId = department.id,
                    firstName = "Petr",
                    lastName = "Smirnov",
                ),
            )
        }
    }

    @Test
    fun `missing department id fails by foreign key in jpa schema`() {
        assertThrows<DataIntegrityViolationException> {
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = UUID.randomUUID(),
                    firstName = "No",
                    lastName = "Department",
                ),
            )
        }
    }

    @Test
    fun `unknown user id is allowed in jpa schema`() {
        // FK to users is verified in migration tests against Flyway schema.
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Ops"))
        val saved =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    userId = UUID.randomUUID(),
                    departmentId = department.id,
                    firstName = "Ghost",
                    lastName = "User",
                ),
            )
        assertNotNull(saved.id)
    }

    @Test
    fun `department id is generated as uuid v4`() {
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Legal"))

        assertNotNull(department.id)
        assertEquals(4, department.id!!.version())
        assertTrue(employeeRepository.findAllByDepartmentId(department.id!!).isEmpty())
    }
}

package com.example.vaccine.api

import com.example.audit.log.AuditAction
import com.example.audit.log.AuditEntityType
import com.example.audit.log.AuditLogRepository
import com.example.auth.role.RoleEntity
import com.example.auth.role.RoleRepository
import com.example.auth.role.UserRoleEntity
import com.example.auth.role.UserRoleId
import com.example.auth.role.UserRoleRepository
import com.example.auth.user.UserEntity
import com.example.auth.user.UserRepository
import com.example.employee.department.DepartmentEntity
import com.example.employee.department.DepartmentRepository
import com.example.employee.person.EmployeeEntity
import com.example.employee.person.EmployeeRepository
import com.example.vaccine.VaccineTestApplication
import com.example.vaccine.disease.DiseaseEntity
import com.example.vaccine.disease.DiseaseRepository
import com.example.vaccine.vaccine.VaccineEntity
import com.example.vaccine.vaccine.VaccineRepository
import com.example.vaccine.vaccinedisease.VaccineDiseaseEntity
import com.example.vaccine.vaccinedisease.VaccineDiseaseId
import com.example.vaccine.vaccinedisease.VaccineDiseaseRepository
import com.example.vaccination.vaccination.VaccinationEntity
import com.example.vaccination.vaccination.VaccinationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [VaccineTestApplication::class])
class VaccineApiTest {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var vaccineRepository: VaccineRepository

    @Autowired
    private lateinit var diseaseRepository: DiseaseRepository

    @Autowired
    private lateinit var vaccineDiseaseRepository: VaccineDiseaseRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var vaccinationRepository: VaccinationRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        cleanup()
    }

    @Test
    fun `read endpoints require authentication`() {
        mockMvc
            .perform(get("/vaccines"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `write is forbidden for person and allowed for medical`() {
        val person = createUserWithRole("PERSON")
        val medical = createUserWithRole("MEDICAL")

        mockMvc
            .perform(
                post("/vaccines")
                    .header("X-Auth-Token", person.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccineBody("FluShield")),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(
                post("/vaccines")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccineBody("FluShield")),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("FluShield"))
    }

    @Test
    fun `medical can create disease and vaccine disease link`() {
        val medical = createUserWithRole("MEDICAL")
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "LinkVax", validityDays = 365, dosesRequired = 1))

        mockMvc
            .perform(
                post("/diseases")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Influenza"}"""),
            ).andExpect(status().isCreated)

        val disease = diseaseRepository.findByName("Influenza")!!

        mockMvc
            .perform(
                post("/vaccines/${vaccine.id}/diseases/${disease.id}")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                get("/vaccines/${vaccine.id}/diseases")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `duplicate vaccine disease link returns conflict`() {
        val medical = createUserWithRole("MEDICAL")
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "DupVax", validityDays = 365, dosesRequired = 1))
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "DupDisease"))
        vaccineDiseaseRepository.saveAndFlush(VaccineDiseaseEntity(id = VaccineDiseaseId(vaccine.id, disease.id)))

        mockMvc
            .perform(
                post("/vaccines/${vaccine.id}/diseases/${disease.id}")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    @Test
    fun `unknown ids in link creation return bad request`() {
        val medical = createUserWithRole("MEDICAL")

        mockMvc
            .perform(
                post("/vaccines/${UUID.randomUUID()}/diseases/12345")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `delete vaccine with existing links returns conflict`() {
        val medical = createUserWithRole("MEDICAL")
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "DeleteVax", validityDays = 365, dosesRequired = 1))
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "DeleteDisease"))
        vaccineDiseaseRepository.saveAndFlush(VaccineDiseaseEntity(id = VaccineDiseaseId(vaccine.id, disease.id)))

        mockMvc
            .perform(
                delete("/vaccines/${vaccine.id}")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    @Test
    fun `rename disease linked to used vaccine returns conflict`() {
        val medical = createUserWithRole("MEDICAL")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Medical"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Ivan",
                    lastName = "Ivanov",
                ),
            )
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "LinkedVax", validityDays = 365, dosesRequired = 1))
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "OriginalDisease"))
        vaccineDiseaseRepository.saveAndFlush(VaccineDiseaseEntity(id = VaccineDiseaseId(vaccine.id, disease.id)))
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = employee.id,
                vaccineId = vaccine.id,
                performedBy = medical.id,
                vaccinationDate = LocalDate.of(2026, 3, 1),
                expirationDate = LocalDate.of(2027, 3, 1),
            ),
        )

        mockMvc
            .perform(
                put("/diseases/${disease.id}")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"RenamedDisease","description":"updated"}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    @Test
    fun `rename disease linked to unused vaccine is allowed`() {
        val medical = createUserWithRole("MEDICAL")
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "UnusedVax", validityDays = 365, dosesRequired = 1))
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "UnusedDisease"))
        vaccineDiseaseRepository.saveAndFlush(VaccineDiseaseEntity(id = VaccineDiseaseId(vaccine.id, disease.id)))

        mockMvc
            .perform(
                put("/diseases/${disease.id}")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"RenamedUnusedDisease","description":"updated"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("RenamedUnusedDisease"))
    }

    @Test
    fun `delete disease link from used vaccine returns conflict`() {
        val medical = createUserWithRole("MEDICAL")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Vaccination"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Petr",
                    lastName = "Petrov",
                ),
            )
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "UsedLinkVax", validityDays = 365, dosesRequired = 1))
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "UsedLinkDisease"))
        vaccineDiseaseRepository.saveAndFlush(VaccineDiseaseEntity(id = VaccineDiseaseId(vaccine.id, disease.id)))
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = employee.id,
                vaccineId = vaccine.id,
                performedBy = medical.id,
                vaccinationDate = LocalDate.of(2026, 3, 2),
                expirationDate = LocalDate.of(2027, 3, 2),
            ),
        )

        mockMvc
            .perform(
                delete("/vaccines/${vaccine.id}/diseases/${disease.id}")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    @Test
    fun `delete disease link from unused vaccine is allowed`() {
        val medical = createUserWithRole("MEDICAL")
        val vaccine = vaccineRepository.saveAndFlush(VaccineEntity(name = "UnusedLinkVax", validityDays = 365, dosesRequired = 1))
        val disease = diseaseRepository.saveAndFlush(DiseaseEntity(name = "UnusedLinkDisease"))
        vaccineDiseaseRepository.saveAndFlush(VaccineDiseaseEntity(id = VaccineDiseaseId(vaccine.id, disease.id)))

        mockMvc
            .perform(
                delete("/vaccines/${vaccine.id}/diseases/${disease.id}")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isNoContent)
    }

    @Test
    fun `update used vaccine fields except active returns conflict`() {
        val medical = createUserWithRole("MEDICAL")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Used Vaccine Department"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Sergey",
                    lastName = "Sergeev",
                ),
            )
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "LockedVax",
                    manufacturer = "Acme",
                    validityDays = 365,
                    dosesRequired = 1,
                    isActive = true,
                ),
            )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = employee.id,
                vaccineId = vaccine.id,
                performedBy = medical.id,
                vaccinationDate = LocalDate.of(2026, 3, 3),
                expirationDate = LocalDate.of(2027, 3, 3),
            ),
        )

        mockMvc
            .perform(
                put("/vaccines/${vaccine.id}")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccineBody(name = "LockedVax", manufacturer = "Other", isActive = true)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("HTTP_409"))
    }

    @Test
    fun `update used vaccine active flag is allowed`() {
        val medical = createUserWithRole("MEDICAL")
        val department = departmentRepository.saveAndFlush(DepartmentEntity(name = "Used Vaccine Toggle"))
        val employee =
            employeeRepository.saveAndFlush(
                EmployeeEntity(
                    departmentId = department.id,
                    firstName = "Alex",
                    lastName = "Alexeev",
                ),
            )
        val vaccine =
            vaccineRepository.saveAndFlush(
                VaccineEntity(
                    name = "ToggleVax",
                    manufacturer = "Acme",
                    validityDays = 365,
                    dosesRequired = 1,
                    isActive = true,
                ),
            )
        vaccinationRepository.saveAndFlush(
            VaccinationEntity(
                employeeId = employee.id,
                vaccineId = vaccine.id,
                performedBy = medical.id,
                vaccinationDate = LocalDate.of(2026, 3, 4),
                expirationDate = LocalDate.of(2027, 3, 4),
            ),
        )

        mockMvc
            .perform(
                put("/vaccines/${vaccine.id}")
                    .header("X-Auth-Token", medical.id.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(vaccineBody(name = "ToggleVax", manufacturer = "Acme", isActive = false)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.isActive").value(false))
    }

    @Test
    fun `write operations create audit records for vaccine disease and links`() {
        val medical = createUserWithRole("MEDICAL")

        val vaccineResponse =
            mockMvc
                .perform(
                    post("/vaccines")
                        .header("X-Auth-Token", medical.id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vaccineBody("AuditVax")),
                ).andExpect(status().isCreated)
                .andReturn()

        val vaccineId = UUID.fromString(extractJsonField(vaccineResponse.response.contentAsString, "id"))

        val diseaseResponse =
            mockMvc
                .perform(
                    post("/diseases")
                        .header("X-Auth-Token", medical.id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"AuditDisease"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val diseaseId = extractJsonField(diseaseResponse.response.contentAsString, "id").toInt()

        mockMvc
            .perform(
                post("/vaccines/$vaccineId/diseases/$diseaseId")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                delete("/vaccines/$vaccineId/diseases/$diseaseId")
                    .header("X-Auth-Token", medical.id.toString()),
            ).andExpect(status().isNoContent)

        val logs = auditLogRepository.findAll()
        org.junit.jupiter.api.Assertions.assertTrue(
            logs.any { it.entityType == AuditEntityType.VACCINE && it.action == AuditAction.CREATE },
        )
        org.junit.jupiter.api.Assertions.assertTrue(
            logs.any { it.entityType == AuditEntityType.DISEASE && it.action == AuditAction.CREATE },
        )
        org.junit.jupiter.api.Assertions.assertTrue(
            logs.any {
                it.entityType == AuditEntityType.VACCINE_DISEASE &&
                    it.action == AuditAction.CREATE
            },
        )
        org.junit.jupiter.api.Assertions.assertTrue(
            logs.any {
                it.entityType == AuditEntityType.VACCINE_DISEASE &&
                    it.action == AuditAction.DELETE
            },
        )
    }

    private fun createUserWithRole(roleCode: String): UserEntity {
        val user = userRepository.saveAndFlush(UserEntity(email = "$roleCode-${UUID.randomUUID()}@example.com", passwordHash = "hash"))
        val role = ensureRole(roleCode)
        userRoleRepository.saveAndFlush(UserRoleEntity(id = UserRoleId(userId = user.id, roleId = role.id)))
        return user
    }

    private fun ensureRole(code: String): RoleEntity =
        roleRepository.findByCode(code)
            ?: roleRepository.saveAndFlush(RoleEntity(code = code, name = code))

    private fun cleanup() {
        auditLogRepository.deleteAll()
        vaccinationRepository.deleteAll()
        vaccineDiseaseRepository.deleteAll()
        employeeRepository.deleteAll()
        departmentRepository.deleteAll()
        vaccineRepository.deleteAll()
        diseaseRepository.deleteAll()
        userRoleRepository.deleteAll()
        roleRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun vaccineBody(
        name: String,
        manufacturer: String = "Acme",
        isActive: Boolean = true,
    ): String =
        """
        {
          "name":"$name",
          "manufacturer":"$manufacturer",
          "validityDays":365,
          "dosesRequired":1,
          "daysBetween":null,
          "isActive":$isActive
        }
        """.trimIndent()

    private fun extractJsonField(
        json: String,
        field: String,
    ): String {
        val regex = """"$field"\s*:\s*("([^"]+)"|(\d+))""".toRegex()
        val match = regex.find(json) ?: error("Field '$field' not found in json: $json")
        return match.groupValues[2].ifBlank { match.groupValues[3] }
    }
}

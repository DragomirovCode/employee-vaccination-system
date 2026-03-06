package com.example.vaccination

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

class MigrationTest {
    @Test
    fun `flyway applies v1 to v5 and enforces vaccination document constraints`() {
        val url = "jdbc:h2:mem:flyway-vaccination;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        val result =
            Flyway
                .configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate()

        assertTrue(result.migrationsExecuted >= 5)

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    "INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at) VALUES (RANDOM_UUID(), 'medic@example.com', 'hash', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                )
                statement.executeUpdate(
                    "INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at) VALUES (RANDOM_UUID(), 'uploader@example.com', 'hash', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                )
                statement.executeUpdate(
                    "INSERT INTO departments (id, name, parent_id, created_at) VALUES (RANDOM_UUID(), 'Medical', NULL, CURRENT_TIMESTAMP)",
                )
                statement.executeUpdate(
                    "INSERT INTO vaccines (id, name, manufacturer, validity_days, doses_required, days_between, is_active, created_at) VALUES (RANDOM_UUID(), 'FluVax', 'ACME', 365, 1, NULL, TRUE, CURRENT_TIMESTAMP)",
                )

                val employeeId = UUID.randomUUID()
                val departmentId =
                    statement.executeQuery("SELECT id FROM departments LIMIT 1").use { rs ->
                        rs.next()
                        rs.getString(1)
                    }
                val performedBy =
                    statement.executeQuery("SELECT id FROM users WHERE email = 'medic@example.com'").use { rs ->
                        rs.next()
                        rs.getString(1)
                    }
                val uploadedBy =
                    statement.executeQuery("SELECT id FROM users WHERE email = 'uploader@example.com'").use { rs ->
                        rs.next()
                        rs.getString(1)
                    }
                val vaccineId =
                    statement.executeQuery("SELECT id FROM vaccines LIMIT 1").use { rs ->
                        rs.next()
                        rs.getString(1)
                    }

                statement.executeUpdate(
                    "INSERT INTO employees (id, user_id, department_id, first_name, last_name, middle_name, birth_date, position, hire_date, created_at, updated_at) VALUES ('$employeeId', NULL, '$departmentId', 'John', 'Doe', NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                )

                val vaccinationId = UUID.randomUUID()
                statement.executeUpdate(
                    "INSERT INTO vaccinations (id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number, expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at) VALUES ('$vaccinationId', '$employeeId', '$vaccineId', '$performedBy', DATE '2026-03-01', 1, 'B-1', NULL, NULL, DATE '2027-03-01', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                )

                statement.executeUpdate(
                    "INSERT INTO documents (id, vaccination_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at) VALUES (RANDOM_UUID(), '$vaccinationId', 'cert.pdf', 'vaccinations/cert.pdf', 1024, 'application/pdf', '$uploadedBy', CURRENT_TIMESTAMP)",
                )

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        "INSERT INTO vaccinations (id, employee_id, vaccine_id, performed_by, vaccination_date, dose_number, batch_number, expiration_date, next_dose_date, revaccination_date, notes, created_at, updated_at) VALUES (RANDOM_UUID(), RANDOM_UUID(), '$vaccineId', '$performedBy', DATE '2026-03-01', 1, NULL, NULL, NULL, DATE '2027-03-01', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    )
                }

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        "INSERT INTO documents (id, vaccination_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at) VALUES (RANDOM_UUID(), RANDOM_UUID(), 'missing.pdf', 'vaccinations/missing.pdf', 1, 'application/pdf', '$uploadedBy', CURRENT_TIMESTAMP)",
                    )
                }
            }
        }
    }
}

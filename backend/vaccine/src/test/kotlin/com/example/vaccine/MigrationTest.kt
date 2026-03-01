package com.example.vaccine

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

class MigrationTest {
    @Test
    fun `flyway applies V4 and enforces vaccine disease constraints`() {
        val url = "jdbc:h2:mem:flyway-vaccine;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        val result =
            Flyway
                .configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate()

        assertTrue(result.migrationsExecuted >= 1)

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    "INSERT INTO diseases (name, description) VALUES ('Polio', 'Poliomyelitis')",
                )

                val vaccineId = UUID.randomUUID()
                statement.executeUpdate(
                    """
                    INSERT INTO vaccines (id, name, manufacturer, validity_days, doses_required, days_between, is_active, created_at)
                    VALUES ('$vaccineId', 'PolioVax', 'ACME', 3650, 3, 30, TRUE, CURRENT_TIMESTAMP)
                    """.trimIndent(),
                )

                val diseaseId =
                    statement.executeQuery("SELECT id FROM diseases WHERE name = 'Polio'").use { rs ->
                        rs.next()
                        rs.getInt(1)
                    }

                statement.executeUpdate(
                    "INSERT INTO vaccine_diseases (vaccine_id, disease_id) VALUES ('$vaccineId', $diseaseId)",
                )

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        "INSERT INTO vaccine_diseases (vaccine_id, disease_id) VALUES ('$vaccineId', $diseaseId)",
                    )
                }

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        "INSERT INTO vaccine_diseases (vaccine_id, disease_id) VALUES ('${UUID.randomUUID()}', $diseaseId)",
                    )
                }
            }
        }
    }
}

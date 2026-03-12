package com.example.app

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class SeedDataMigrationTest {
    @Test
    fun `flyway seeds all application tables with starter data`() {
        val url = "jdbc:h2:mem:flyway-seed-data;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"

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
                val expectedMinimums =
                    mapOf(
                        "roles" to 4,
                        "users" to 5,
                        "user_roles" to 5,
                        "departments" to 7,
                        "employees" to 12,
                        "diseases" to 3,
                        "vaccines" to 3,
                        "vaccine_diseases" to 3,
                        "vaccinations" to 6,
                        "documents" to 4,
                        "notifications" to 2,
                        "audit_log" to 4,
                    )

                expectedMinimums.forEach { (tableName, minCount) ->
                    val actual =
                        statement.executeQuery("SELECT COUNT(*) FROM $tableName").use { rs ->
                            rs.next()
                            rs.getInt(1)
                        }
                    assertTrue(actual >= minCount, "Expected at least $minCount rows in $tableName, got $actual")
                }

                val adminEmail =
                    statement
                        .executeQuery(
                            """
                            SELECT email
                            FROM users
                            WHERE id = '018f4fd2-75f8-7f2e-b95e-9df7ac8e3a10'
                            """.trimIndent(),
                        ).use { rs ->
                            rs.next()
                            rs.getString(1)
                        }
                assertEquals("admin@evs.local", adminEmail)

                val dueNotificationCount =
                    statement
                        .executeQuery(
                            """
                            SELECT COUNT(*)
                            FROM notifications
                            WHERE type = 'REVACCINATION_DUE'
                            """.trimIndent(),
                        ).use { rs ->
                            rs.next()
                            rs.getInt(1)
                        }
                assertTrue(dueNotificationCount >= 2)
            }
        }
    }
}

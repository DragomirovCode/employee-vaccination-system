package com.example.audit

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

class MigrationTest {
    /**
     * Проверяет, что миграция V6 создает таблицу аудита и включает внешний ключ на таблицу пользователей.
     */
    @Test
    fun `flyway applies V6 and enforces audit user foreign key`() {
        val url = "jdbc:h2:mem:flyway-audit;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE users (
                        id UUID PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        is_active BOOLEAN NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val result =
            Flyway
                .configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load()
                .migrate()

        assertTrue(result.migrationsExecuted >= 1)

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                val userId = UUID.randomUUID()
                val entityId = UUID.randomUUID()
                statement.executeUpdate(
                    "INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at) VALUES ('$userId', 'audit@example.com', 'hash', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                )
                statement.executeUpdate(
                    "INSERT INTO audit_log (id, user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES (RANDOM_UUID(), '$userId', 'CREATE', 'VACCINATION', '$entityId', NULL, '{\"id\":\"$entityId\"}', CURRENT_TIMESTAMP)",
                )

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        "INSERT INTO audit_log (id, user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES (RANDOM_UUID(), RANDOM_UUID(), 'DELETE', 'DOCUMENT', RANDOM_UUID(), '{\"a\":1}', NULL, CURRENT_TIMESTAMP)",
                    )
                }
            }
        }
    }
}

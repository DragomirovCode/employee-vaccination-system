package com.example.employee

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.DriverManager
import java.sql.SQLException

class MigrationTest {
    @Test
    fun `flyway applies v1 v2 v3 and creates employee schema constraints`() {
        val url = "jdbc:h2:mem:flyway-employee;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        val result =
            Flyway
                .configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate()

        assertTrue(result.migrationsExecuted >= 3)

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    INSERT INTO departments (id, name, parent_id, created_at)
                    VALUES (RANDOM_UUID(), 'Root', NULL, CURRENT_TIMESTAMP)
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    INSERT INTO users (id, email, password_hash, is_active, created_at, updated_at)
                    VALUES (RANDOM_UUID(), 'migration-user@example.com', 'hash', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """.trimIndent(),
                )

                val departmentId =
                    statement.executeQuery("SELECT id FROM departments LIMIT 1").use { rs ->
                        rs.next()
                        rs.getString(1)
                    }
                val userId =
                    statement.executeQuery("SELECT id FROM users LIMIT 1").use { rs ->
                        rs.next()
                        rs.getString(1)
                    }

                statement.executeUpdate(
                    """
                    INSERT INTO employees (
                        id, user_id, department_id, first_name, last_name,
                        middle_name, birth_date, position, hire_date, created_at, updated_at
                    ) VALUES (
                        RANDOM_UUID(), '$userId', '$departmentId', 'A', 'B',
                        NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """.trimIndent(),
                )

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        """
                        INSERT INTO employees (
                            id, user_id, department_id, first_name, last_name,
                            middle_name, birth_date, position, hire_date, created_at, updated_at
                        ) VALUES (
                            RANDOM_UUID(), '$userId', '$departmentId', 'C', 'D',
                            NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        """.trimIndent(),
                    )
                }

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        """
                        INSERT INTO employees (
                            id, user_id, department_id, first_name, last_name,
                            middle_name, birth_date, position, hire_date, created_at, updated_at
                        ) VALUES (
                            RANDOM_UUID(), RANDOM_UUID(), '$departmentId', 'E', 'F',
                            NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        """.trimIndent(),
                    )
                }

                assertThrows<SQLException> {
                    statement.executeUpdate(
                        """
                        INSERT INTO employees (
                            id, user_id, department_id, first_name, last_name,
                            middle_name, birth_date, position, hire_date, created_at, updated_at
                        ) VALUES (
                            RANDOM_UUID(), NULL, RANDOM_UUID(), 'G', 'H',
                            NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        """.trimIndent(),
                    )
                }
            }
        }
    }
}

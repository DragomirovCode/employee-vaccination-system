package com.example.auth.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException

@SpringBootTest
class UserRepositoryTest {
    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    @Test
    fun `save and find by email`() {
        val saved =
            userRepository.save(
                UserEntity(
                    email = "user@example.com",
                    passwordHash = "hash",
                ),
            )

        val found = userRepository.findByEmail("user@example.com")

        assertEquals(saved.id, found?.id)
        assertTrue(userRepository.existsByEmail("user@example.com"))
    }

    @Test
    fun `duplicate email is rejected by unique constraint`() {
        userRepository.saveAndFlush(
            UserEntity(
                email = "dup@example.com",
                passwordHash = "hash-1",
            ),
        )

        assertThrows<DataIntegrityViolationException> {
            userRepository.saveAndFlush(
                UserEntity(
                    email = "dup@example.com",
                    passwordHash = "hash-2",
                ),
            )
        }
    }
}

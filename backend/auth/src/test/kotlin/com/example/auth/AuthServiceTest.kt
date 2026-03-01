package com.example.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthServiceTest {
    private val service = AuthService()

    @Test
    fun `valid token`() {
        assertTrue(service.isTokenValid("dev-token"))
    }

    @Test
    fun `invalid token`() {
        assertFalse(service.isTokenValid("wrong"))
        assertFalse(service.isTokenValid(null))
    }
}

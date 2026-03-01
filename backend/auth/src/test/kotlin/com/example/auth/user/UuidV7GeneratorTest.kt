package com.example.auth.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UuidV7GeneratorTest {
    @Test
    fun `generator creates version 7 uuid`() {
        val id = UuidV7Generator.next()
        assertEquals(7, id.version())
    }
}

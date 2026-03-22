package com.example.employee.common

import java.util.UUID

object UuidV4Generator {
    /**
     * Генерирует случайный UUID версии 4.
     *
     * @return новый UUID v4
     */
    fun next(): UUID = UUID.randomUUID()
}

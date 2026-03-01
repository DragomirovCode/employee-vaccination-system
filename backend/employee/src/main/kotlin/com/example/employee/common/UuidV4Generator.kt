package com.example.employee.common

import java.util.UUID

object UuidV4Generator {
    fun next(): UUID = UUID.randomUUID()
}

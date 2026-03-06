package com.example.vaccination.common

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

object UuidV7Generator {
    fun next(): UUID = UuidCreator.getTimeOrderedEpoch()
}

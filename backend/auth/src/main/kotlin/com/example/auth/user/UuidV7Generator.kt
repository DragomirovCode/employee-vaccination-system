package com.example.auth.user

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

object UuidV7Generator {
    /**
     * Генерирует UUID версии 7 с временной сортировкой.
     *
     * @return новый UUID v7
     */
    fun next(): UUID = UuidCreator.getTimeOrderedEpoch()
}

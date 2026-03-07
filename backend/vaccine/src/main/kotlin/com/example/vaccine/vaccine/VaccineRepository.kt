package com.example.vaccine.vaccine

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VaccineRepository : JpaRepository<VaccineEntity, UUID> {
    fun findByName(name: String): VaccineEntity?
}

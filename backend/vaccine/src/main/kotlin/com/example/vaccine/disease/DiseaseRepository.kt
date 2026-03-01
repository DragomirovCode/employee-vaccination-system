package com.example.vaccine.disease

import org.springframework.data.jpa.repository.JpaRepository

interface DiseaseRepository : JpaRepository<DiseaseEntity, Int> {
    fun findByName(name: String): DiseaseEntity?
}

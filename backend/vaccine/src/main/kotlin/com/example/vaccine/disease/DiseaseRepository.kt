package com.example.vaccine.disease

import org.springframework.data.jpa.repository.JpaRepository

interface DiseaseRepository : JpaRepository<DiseaseEntity, Int> {
    /**
     * Ищет заболевание по имени.
     */
    fun findByName(name: String): DiseaseEntity?
}

package com.example.vaccine.vaccinedisease

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface VaccineDiseaseRepository : JpaRepository<VaccineDiseaseEntity, VaccineDiseaseId> {
    fun findAllByIdVaccineId(vaccineId: UUID): List<VaccineDiseaseEntity>

    fun existsByIdVaccineId(vaccineId: UUID): Boolean

    fun existsByIdDiseaseId(diseaseId: Int): Boolean

    fun existsByIdVaccineIdAndIdDiseaseId(
        vaccineId: UUID,
        diseaseId: Int,
    ): Boolean

    fun deleteByIdVaccineIdAndIdDiseaseId(
        vaccineId: UUID,
        diseaseId: Int,
    ): Long

    @Query(
        value =
            """
            SELECT EXISTS (
                SELECT 1
                FROM vaccine_diseases vd
                JOIN vaccinations v ON v.vaccine_id = vd.vaccine_id
                WHERE vd.disease_id = :diseaseId
            )
            """,
        nativeQuery = true,
    )
    fun existsUsedVaccineLinkByDiseaseId(
        @Param("diseaseId") diseaseId: Int,
    ): Boolean

    @Query(
        value =
            """
            SELECT EXISTS (
                SELECT 1
                FROM vaccinations v
                WHERE v.vaccine_id = :vaccineId
            )
            """,
        nativeQuery = true,
    )
    fun existsVaccinationByVaccineId(
        @Param("vaccineId") vaccineId: UUID,
    ): Boolean
}

package com.example.vaccine.vaccinedisease

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface VaccineDiseaseRepository : JpaRepository<VaccineDiseaseEntity, VaccineDiseaseId> {
    /**
     * Возвращает все связи для указанной вакцины.
     */
    fun findAllByIdVaccineId(vaccineId: UUID): List<VaccineDiseaseEntity>

    /**
     * Проверяет наличие связей у вакцины.
     */
    fun existsByIdVaccineId(vaccineId: UUID): Boolean

    /**
     * Проверяет наличие связей у заболевания.
     */
    fun existsByIdDiseaseId(diseaseId: Int): Boolean

    /**
     * Проверяет существование конкретной связи вакцины и заболевания.
     */
    fun existsByIdVaccineIdAndIdDiseaseId(
        vaccineId: UUID,
        diseaseId: Int,
    ): Boolean

    /**
     * Удаляет связь вакцины и заболевания.
     *
     * @return количество удаленных записей
     */
    fun deleteByIdVaccineIdAndIdDiseaseId(
        vaccineId: UUID,
        diseaseId: Int,
    ): Long

    /**
     * Проверяет, использовалась ли хотя бы одна вакцина, связанная с указанным заболеванием.
     */
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

    /**
     * Проверяет, использовалась ли вакцина в записях о вакцинации.
     */
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

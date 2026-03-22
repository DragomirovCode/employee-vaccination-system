package com.example.vaccination.document

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentRepository : JpaRepository<DocumentEntity, UUID> {
    /**
     * Возвращает все документы, связанные с указанной записью вакцинации.
     */
    fun findAllByVaccinationId(vaccinationId: UUID): List<DocumentEntity>

    /**
     * Возвращает документы вакцинации, отсортированные по времени загрузки от новых к старым.
     */
    fun findAllByVaccinationIdOrderByUploadedAtDesc(vaccinationId: UUID): List<DocumentEntity>
}

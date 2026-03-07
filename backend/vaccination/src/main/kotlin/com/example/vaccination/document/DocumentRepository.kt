package com.example.vaccination.document

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentRepository : JpaRepository<DocumentEntity, UUID> {
    fun findAllByVaccinationId(vaccinationId: UUID): List<DocumentEntity>

    fun findAllByVaccinationIdOrderByUploadedAtDesc(vaccinationId: UUID): List<DocumentEntity>
}

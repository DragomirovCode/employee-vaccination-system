package com.example.vaccine.disease

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * JPA-сущность заболевания.
 */
@Entity
@Table(name = "diseases")
class DiseaseEntity(
    /** Числовой идентификатор заболевания. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    /** Наименование заболевания. */
    @Column(nullable = false, unique = true, length = 255)
    var name: String = "",
    /** Текстовое описание заболевания. */
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
)

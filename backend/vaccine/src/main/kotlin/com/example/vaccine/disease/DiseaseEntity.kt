package com.example.vaccine.disease

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "diseases")
class DiseaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    @Column(nullable = false, unique = true, length = 255)
    var name: String = "",
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
)

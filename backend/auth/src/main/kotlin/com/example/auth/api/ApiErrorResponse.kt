package com.example.auth.api

import java.time.Instant

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val details: List<String>? = null,
    val path: String,
    val timestamp: Instant,
    val traceId: String? = null,
)

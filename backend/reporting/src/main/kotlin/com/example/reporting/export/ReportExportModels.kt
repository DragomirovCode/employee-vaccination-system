package com.example.reporting.export

enum class ReportFormat {
    CSV,
    XLSX,
    PDF,
    ;

    companion object {
        fun fromRaw(raw: String): ReportFormat? =
            when (raw.trim().lowercase()) {
                "csv" -> CSV
                "xlsx" -> XLSX
                "pdf" -> PDF
                else -> null
            }
    }
}

data class ReportFile(
    val bytes: ByteArray,
    val contentType: String,
    val fileName: String,
)

package com.example.reporting.export

enum class ReportFormat {
    CSV,
    XLSX,
    PDF,
    ;

    companion object {
        /**
         * Преобразует строковое значение формата в поддерживаемый enum.
         */
        fun fromRaw(raw: String): ReportFormat? =
            when (raw.trim().lowercase()) {
                "csv" -> CSV
                "xlsx" -> XLSX
                "pdf" -> PDF
                else -> null
            }
    }
}

/**
 * Файл сформированного отчета вместе с MIME-типом и именем файла.
 */
data class ReportFile(
    /** Содержимое файла в байтах. */
    val bytes: ByteArray,
    /** MIME-тип файла. */
    val contentType: String,
    /** Имя файла для скачивания. */
    val fileName: String,
)

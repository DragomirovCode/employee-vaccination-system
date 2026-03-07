package com.example.reporting.export

import org.springframework.stereotype.Component

@Component
class CsvReportExporter {
    fun export(
        headers: List<String>,
        rows: List<List<Any?>>,
    ): ByteArray {
        val builder = StringBuilder()
        builder.appendLine(headers.joinToString(",") { escape(it) })
        rows.forEach { row ->
            builder.appendLine(row.joinToString(",") { escape(it?.toString() ?: "") })
        }
        return builder.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escape(value: String): String {
        val mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        if (!mustQuote) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}

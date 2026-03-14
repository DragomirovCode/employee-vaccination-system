package com.example.reporting.export

import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class CsvReportExporter {
    fun export(
        headers: List<String>,
        rows: List<List<Any?>>,
        fileNameBase: String,
    ): ReportFile {
        val builder = StringBuilder()
        builder.appendLine(headers.joinToString(",") { escape(it) })
        rows.forEach { row ->
            builder.appendLine(row.joinToString(",") { escape(it?.toString() ?: "") })
        }
        val bytes =
            ByteArrayOutputStream().use { output ->
                // UTF-8 BOM helps Excel/Windows open localized CSV files with the correct encoding.
                output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                output.write(builder.toString().toByteArray(Charsets.UTF_8))
                output.toByteArray()
            }

        return ReportFile(
            bytes = bytes,
            contentType = "text/csv; charset=UTF-8",
            fileName = "$fileNameBase.csv",
        )
    }

    private fun escape(value: String): String {
        val mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        if (!mustQuote) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}

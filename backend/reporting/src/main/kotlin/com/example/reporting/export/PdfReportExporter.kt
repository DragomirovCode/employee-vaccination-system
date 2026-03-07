package com.example.reporting.export

import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.FontFactory
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class PdfReportExporter {
    fun export(
        headers: List<String>,
        rows: List<List<Any?>>,
        fileNameBase: String,
    ): ReportFile {
        val bytes =
            ByteArrayOutputStream().use { output ->
                val document = Document()
                try {
                    PdfWriter.getInstance(document, output)
                    document.open()
                    document.add(Paragraph(fileNameBase.replace("-", " ").replaceFirstChar { it.uppercase() }))
                    document.add(Paragraph(" "))
                    document.add(createTable(headers, rows))
                    document.close()
                    output.toByteArray()
                } catch (ex: DocumentException) {
                    throw IllegalStateException("Failed to generate PDF report", ex)
                } finally {
                    if (document.isOpen) {
                        document.close()
                    }
                }
            }

        return ReportFile(
            bytes = bytes,
            contentType = "application/pdf",
            fileName = "$fileNameBase.pdf",
        )
    }

    private fun createTable(
        headers: List<String>,
        rows: List<List<Any?>>,
    ): PdfPTable {
        val table = PdfPTable(headers.size.coerceAtLeast(1))
        table.widthPercentage = 100f

        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)))
            table.addCell(cell)
        }

        rows.forEach { row ->
            row.forEach { value ->
                table.addCell(Phrase(value?.toString() ?: ""))
            }
        }

        return table
    }
}

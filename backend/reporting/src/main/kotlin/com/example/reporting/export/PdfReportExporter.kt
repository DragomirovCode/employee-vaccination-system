package com.example.reporting.export

import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class PdfReportExporter {
    init {
        FontFactory.registerDirectories()
    }

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
        val headerFont = resolveFont(10f, Font.BOLD)
        val bodyFont = resolveFont(10f, Font.NORMAL)

        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header, headerFont))
            table.addCell(cell)
        }

        rows.forEach { row ->
            row.forEach { value ->
                table.addCell(Phrase(value?.toString() ?: "", bodyFont))
            }
        }

        return table
    }

    private fun resolveFont(
        size: Float,
        style: Int,
    ): Font {
        val candidates =
            listOf(
                "Segoe UI",
                "Arial",
                "DejaVu Sans",
                "Liberation Sans",
            )

        candidates.forEach { candidate ->
            val font = FontFactory.getFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style)
            if (font.baseFont != null) {
                return font
            }
        }

        return FontFactory.getFont(FontFactory.HELVETICA, size, style)
    }
}

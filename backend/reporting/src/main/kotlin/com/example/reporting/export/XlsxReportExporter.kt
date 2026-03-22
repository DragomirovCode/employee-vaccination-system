package com.example.reporting.export

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class XlsxReportExporter {
    /**
     * Формирует XLSX-файл отчета и возвращает его содержимое с метаданными.
     */
    fun export(
        headers: List<String>,
        rows: List<List<Any?>>,
        fileNameBase: String,
    ): ReportFile {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("report")

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        rows.forEachIndexed { rowIndex, row ->
            val excelRow = sheet.createRow(rowIndex + 1)
            row.forEachIndexed { cellIndex, value ->
                excelRow.createCell(cellIndex).setCellValue(value?.toString() ?: "")
            }
        }

        headers.indices.forEach(sheet::autoSizeColumn)

        val bytes =
            ByteArrayOutputStream().use { output ->
                workbook.use { it.write(output) }
                output.toByteArray()
            }

        return ReportFile(
            bytes = bytes,
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            fileName = "$fileNameBase.xlsx",
        )
    }
}

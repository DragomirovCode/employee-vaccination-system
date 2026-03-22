package com.example.reporting.export

import org.springframework.stereotype.Service

@Service
class ReportExportService(
    private val csvReportExporter: CsvReportExporter,
    private val xlsxReportExporter: XlsxReportExporter,
    private val pdfReportExporter: PdfReportExporter,
) {
    /**
     * Делегирует формирование файла соответствующему экспортеру в зависимости от формата.
     */
    fun export(
        format: ReportFormat,
        fileNameBase: String,
        headers: List<String>,
        rows: List<List<Any?>>,
    ): ReportFile =
        when (format) {
            ReportFormat.CSV -> csvReportExporter.export(headers = headers, rows = rows, fileNameBase = fileNameBase)
            ReportFormat.XLSX -> xlsxReportExporter.export(headers = headers, rows = rows, fileNameBase = fileNameBase)
            ReportFormat.PDF -> pdfReportExporter.export(headers = headers, rows = rows, fileNameBase = fileNameBase)
        }
}

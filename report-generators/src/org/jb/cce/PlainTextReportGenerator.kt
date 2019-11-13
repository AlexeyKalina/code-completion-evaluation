package org.jb.cce;

import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlainTextReportGenerator(outputDir: String, filterName: String): ReportGenerator {
    companion object {
        private const val globalReportName: String = "report.txt"
        private const val reportType: String = "plain"
    }

    private val filterDir = Paths.get(outputDir, reportType, filterName)

    init {
        Files.createDirectories(filterDir)
    }

    override fun generateFileReport(sessions: List<FileEvaluationInfo>) = Unit
    override fun generateErrorReports(errors: List<FileErrorInfo>) = Unit

    override fun generateGlobalReport(globalMetrics: List<MetricInfo>): Path {
        val reportPath = Paths.get(filterDir.toString(), globalReportName)
        FileWriter(reportPath.toString()).use {
            it.write(globalMetrics.joinToString("\n") { "${it.evaluationType} ${it.name}: ${it.value}" })
        }
        return reportPath
    }
}

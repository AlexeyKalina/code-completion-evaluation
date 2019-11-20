package org.jb.cce;

import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class PlainTextReportGenerator(outputDir: String, filterName: String): ReportGenerator {
    companion object {
        private const val globalReportName: String = "report.txt"
    }

    override val type: String = "plain"
    private val filterDir = Paths.get(outputDir, type, filterName)

    init {
        Files.createDirectories(filterDir)
    }

    override fun generateFileReport(sessions: List<FileEvaluationInfo>) = Unit
    override fun generateErrorReports(errors: List<FileErrorInfo>) = Unit

    override fun generateGlobalReport(globalMetrics: List<MetricInfo>): Path {
        val reportPath = Paths.get(filterDir.toString(), globalReportName)
        FileWriter(reportPath.toString()).use { writer ->
            writer.write(globalMetrics
                    .filter { !it.name.contains("latency", ignoreCase = true) }
                    .joinToString("\n") { "${it.evaluationType} ${it.name}: ${"%.3f".format(Locale.US, it.value)}" })
        }
        return reportPath
    }
}

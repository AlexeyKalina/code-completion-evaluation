package org.jb.cce

import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import java.nio.file.Path

interface ReportGenerator {
    val type: String
    fun generateFileReport(sessions: List<FileEvaluationInfo>)
    fun generateErrorReports(errors: List<FileErrorInfo>)
    fun generateGlobalReport(globalMetrics: List<MetricInfo>): Path
}
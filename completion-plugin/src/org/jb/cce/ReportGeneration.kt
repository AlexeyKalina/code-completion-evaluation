package org.jb.cce

import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.info.MetricsEvaluationInfo
import org.jb.cce.info.SessionsEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import org.jb.cce.metrics.MetricsEvaluator

fun generateReport(reportGenerator: HtmlReportGenerator, sessionsInfo: List<SessionsEvaluationInfo>, errors: List<FileErrorInfo>): String {
    val metricsInfo = evaluateMetrics(sessionsInfo)
    return reportGenerator.generateReport(sessionsInfo, metricsInfo, errors)
}

private fun evaluateMetrics(evaluationsInfo: List<SessionsEvaluationInfo>): List<MetricsEvaluationInfo> {
    val metricsInfo = mutableListOf<MetricsEvaluationInfo>()
    for (sessionsInfo in evaluationsInfo) {
        val metricsEvaluator = MetricsEvaluator.withDefaultMetrics()
        val filesInfo = mutableListOf<FileEvaluationInfo<MetricInfo>>()
        for (file in sessionsInfo.sessions) {
            filesInfo.add(FileEvaluationInfo(file.filePath, metricsEvaluator.evaluate(file.results), file.text))
        }
        metricsInfo.add(MetricsEvaluationInfo(metricsEvaluator.result(), filesInfo, sessionsInfo.info))
    }
    return metricsInfo
}

package org.jb.cce.info

import org.jb.cce.metrics.MetricInfo

data class MetricsEvaluationInfo(val globalMetrics: List<MetricInfo>, val fileMetrics: List<FileEvaluationInfo<MetricInfo>>, val info: EvaluationInfo)
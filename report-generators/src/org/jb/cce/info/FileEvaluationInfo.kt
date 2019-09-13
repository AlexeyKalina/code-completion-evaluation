package org.jb.cce.info

import org.jb.cce.metrics.MetricInfo

data class FileEvaluationInfo(val sessionsInfo: FileSessionsInfo, val metrics: List<MetricInfo>, val evaluationType: String)
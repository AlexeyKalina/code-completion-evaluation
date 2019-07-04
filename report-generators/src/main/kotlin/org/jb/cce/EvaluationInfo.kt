package org.jb.cce

import org.jb.cce.metrics.MetricInfo

data class EvaluationInfo (val evaluationType: String, val filesInfo: Map<String, FileEvaluationInfo>, val metrics: List<MetricInfo>)
package org.jb.cce

import org.jb.cce.metrics.MetricInfo

data class FileEvaluationInfo(val sessions: List<Session>, val metrics: List<MetricInfo>, val text: String)
package org.jb.cce

import org.jb.cce.metrics.MetricInfo

class EvaluationInfo (val evaluationType: String) {
    private val _filesInfo = mutableMapOf<String, FileEvaluationInfo>()

    val filesInfo: Map<String, FileEvaluationInfo> = _filesInfo
    lateinit var metrics: List<MetricInfo>

    fun addFileInfo(name: String, evaluationInfo: FileEvaluationInfo) {
        _filesInfo.put(name, evaluationInfo)
    }
}
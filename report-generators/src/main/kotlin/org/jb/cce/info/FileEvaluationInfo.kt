package org.jb.cce.info

data class FileEvaluationInfo<T>(val filePath: String, val results: List<T>, val text: String)
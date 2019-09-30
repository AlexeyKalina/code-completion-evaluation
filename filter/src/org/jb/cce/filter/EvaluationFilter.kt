package org.jb.cce.filter

import org.jb.cce.uast.NodeProperties


interface EvaluationFilter {
  companion object {
    val ACCEPT_ALL = object : EvaluationFilter {
      override fun shouldEvaluate(properties: NodeProperties): Boolean = true
    }

    fun any(vararg filters: EvaluationFilter): EvaluationFilter = object : EvaluationFilter {
      override fun shouldEvaluate(properties: NodeProperties): Boolean {
        return filters.any { it.shouldEvaluate(properties) }
      }
    }

    fun all(vararg filters: EvaluationFilter): EvaluationFilter = object : EvaluationFilter {
      override fun shouldEvaluate(properties: NodeProperties): Boolean {
        return filters.all { it.shouldEvaluate(properties) }
      }
    }
  }

  fun shouldEvaluate(properties: NodeProperties): Boolean
}

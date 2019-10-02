package org.jb.cce.filter

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import org.jb.cce.uast.NodeProperties

interface EvaluationFilter {
  companion object {
    val ACCEPT_ALL = object : EvaluationFilter {
      override fun shouldEvaluate(properties: NodeProperties): Boolean = true
      override fun toJson(): JsonElement = JsonNull.INSTANCE
    }
  }

  fun shouldEvaluate(properties: NodeProperties): Boolean

  fun toJson(): JsonElement
}

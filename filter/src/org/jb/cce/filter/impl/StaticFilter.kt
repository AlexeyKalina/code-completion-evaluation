package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.uast.NodeProperties

class StaticFilter(private val value: Boolean) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean {
        val isStatic = properties.isStatic ?: return true
        return isStatic && value || !isStatic && !value
    }
}
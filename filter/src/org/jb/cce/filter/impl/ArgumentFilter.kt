package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.uast.NodeProperties

class ArgumentFilter(private val value: Boolean) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean {
        val isArgument = properties.isArgument ?: return true
        return isArgument && value || !isArgument && !value
    }
}
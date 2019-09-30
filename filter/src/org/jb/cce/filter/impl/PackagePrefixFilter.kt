package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.uast.NodeProperties

class PackagePrefixFilter(private val value: String) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean {
        val packageName = properties.packageName ?: return true
        return packageName.startsWith(value)
    }
}
package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.uast.NodeProperties
import org.jb.cce.uast.TypeProperty

class TypeFilter(private val values: List<FilterValue>) : EvaluationFilter {
    enum class FilterValue {
        VARIABLES,
        METHOD_CALLS,
        FIELDS
    }

    override fun shouldEvaluate(properties: NodeProperties): Boolean {
        when (properties.tokenType) {
            TypeProperty.METHOD_CALL -> if (!values.contains(FilterValue.METHOD_CALLS)) return false
            TypeProperty.VARIABLE -> if (!values.contains(FilterValue.VARIABLES)) return false
            TypeProperty.FIELD -> if (!values.contains(FilterValue.FIELDS)) return false
        }
        return true
    }
}
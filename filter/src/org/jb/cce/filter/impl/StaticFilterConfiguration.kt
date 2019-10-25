package org.jb.cce.filter.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties

class StaticFilter(val expectedValue: Boolean) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.isStatic?.equals(expectedValue) ?: true
    override fun toJson(): JsonElement = JsonPrimitive(expectedValue)
}

class StaticFilterConfiguration: EvaluationFilterConfiguration {
    companion object {
        const val id = "isStatic"
    }
    override val id: String = StaticFilterConfiguration.id
    override val description: String = "Filter out token if it's static member access"

    override fun isLanguageSupported(languageName: String): Boolean = Language.JAVA.displayName == languageName

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else StaticFilter(json as Boolean)

    override fun defaultFilter(): EvaluationFilter = StaticFilter(false)
}
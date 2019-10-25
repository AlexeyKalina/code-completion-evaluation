package org.jb.cce.filter.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties

class ArgumentFilter(val expectedValue: Boolean) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.isArgument?.equals(expectedValue) ?: true
    override fun toJson(): JsonElement = JsonPrimitive(expectedValue)
}

class ArgumentFilterConfiguration: EvaluationFilterConfiguration {
    companion object {
        const val id = "isArgument"
    }
    override val id: String = ArgumentFilterConfiguration.id
    override val description: String = "Filter out token if it's method argument"

    override fun isLanguageSupported(languageName: String): Boolean = listOf(Language.JAVA, Language.PYTHON).any { it.displayName == languageName }

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else ArgumentFilter(json as Boolean)

    override fun defaultFilter(): EvaluationFilter = EvaluationFilter.ACCEPT_ALL
}
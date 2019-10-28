package org.jb.cce.filter.impl

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import org.jb.cce.uast.TypeProperty

class TypeFilter(val values: List<TypeProperty>) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.tokenType == null || values.contains(properties.tokenType!!)
    override fun toJson(): JsonElement {
        val json = JsonArray()
        for (value in values)
            json.add(JsonPrimitive(value.name))
        return json
    }
}

class TypeFilterConfiguration : EvaluationFilterConfiguration {
    companion object {
        const val id = "statementTypes"
    }
    override val id: String = TypeFilterConfiguration.id
    override val description: String = "Filter out tokens by statement type"

    override fun isLanguageSupported(languageName: String): Boolean = Language.values().any { it.displayName == languageName }

    override fun buildFromJson(json: Any?): EvaluationFilter =
            if (json == null) EvaluationFilter.ACCEPT_ALL
            else TypeFilter((json as List<String>).map { TypeProperty.valueOf(it) })

    override fun defaultFilter(): EvaluationFilter = TypeFilter(listOf(TypeProperty.METHOD_CALL))
}
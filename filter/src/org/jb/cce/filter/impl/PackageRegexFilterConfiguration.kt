package org.jb.cce.filter.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties

class PackageRegexFilter(pattern: String) : EvaluationFilter {
    val regex = Regex(pattern)
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.packageName?.matches(regex) ?: true
    override fun toJson(): JsonElement = JsonPrimitive(regex.pattern)
}

class PackageRegexFilterConfiguration: EvaluationFilterConfiguration {
    companion object {
        const val id = "packageRegex"
    }
    override val id: String = PackageRegexFilterConfiguration.id
    override val description: String = "Filter out tokens by package name regex"

    override fun isLanguageSupported(languageName: String): Boolean = Language.JAVA.displayName == languageName

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else PackageRegexFilter(json as String)

    override fun defaultFilter(): EvaluationFilter = PackageRegexFilter(".*")
}
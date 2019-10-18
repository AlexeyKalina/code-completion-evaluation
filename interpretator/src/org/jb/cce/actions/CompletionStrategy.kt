package org.jb.cce.actions

import com.google.gson.*
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterManager
import java.lang.reflect.Type

data class CompletionStrategy(val prefix: CompletionPrefix,
                              val context: CompletionContext,
                              val completeAllTokens: Boolean,
                              val filters: Map<String, EvaluationFilter>)

sealed class CompletionPrefix(val emulateTyping: Boolean) {
    object NoPrefix : CompletionPrefix(false)
    class CapitalizePrefix(emulateTyping: Boolean) : CompletionPrefix(emulateTyping)
    class SimplePrefix(emulateTyping: Boolean, val n: Int) : CompletionPrefix(emulateTyping)
}

enum class CompletionType {
    BASIC,
    SMART,
    ML
}

enum class CompletionContext {
    ALL,
    PREVIOUS
}

class CompletionStrategySerializer : JsonSerializer<CompletionStrategy> {
    override fun serialize(src: CompletionStrategy, typeOfSrc: Type, context: JsonSerializationContext): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.addProperty("completeAllTokens", src.completeAllTokens)
        jsonObject.addProperty("context", src.context.name)
        val prefixClassName = src.prefix.javaClass.name
        val prefixObject = JsonObject()
        prefixObject.add("name", JsonPrimitive(prefixClassName.substring(prefixClassName.indexOf('$') + 1)))
        jsonObject.add("prefix", prefixObject)
        if (src.prefix !is CompletionPrefix.NoPrefix) prefixObject.add("emulateTyping", JsonPrimitive(src.prefix.emulateTyping))
        if (src.prefix is CompletionPrefix.SimplePrefix) prefixObject.add("n", JsonPrimitive(src.prefix.n))
        val filtersObject = JsonObject()
        src.filters.forEach { id, filter -> filtersObject.add(id, filter.toJson()) }
        jsonObject.add("filters", filtersObject)
        return jsonObject
    }
}

class CompletionStrategyDeserializer {
    fun deserialize(strategy: Map<String, Any>): CompletionStrategy {
        val evaluationFilters = mutableMapOf<String, EvaluationFilter>()
        val completeAllTokens = strategy.getAs<Boolean>("completeAllTokens")
        if (!completeAllTokens) {
            val filters = strategy.getAs<Map<String, Any>>("filters")
            for ((id, description) in filters) {
                val configuration = EvaluationFilterManager.getConfigurationById(id)
                        ?: throw IllegalStateException("Unknown filter: $id")
                evaluationFilters[id] = configuration.buildFromJson(description)
            }
        }
        return CompletionStrategy(getPrefix(strategy), CompletionContext.valueOf(strategy.getAs("context")), completeAllTokens, evaluationFilters)
    }

    private fun getPrefix(strategy: Map<String, Any>): CompletionPrefix {
        val prefix = strategy.getAs<Map<String, Any>>("prefix")
        return when (prefix["name"]) {
            "NoPrefix" -> CompletionPrefix.NoPrefix
            "CapitalizePrefix" -> CompletionPrefix.CapitalizePrefix(prefix.getAs("emulateTyping"))
            "SimplePrefix" -> CompletionPrefix.SimplePrefix(prefix.getAs("emulateTyping"), prefix.getAs<Double>("n").toInt())
            else -> throw IllegalArgumentException("Unknown completion prefix")
        }
    }
}

inline fun <reified T> Map<String, *>.getAs(key: String): T {
    check(key in this.keys) { "$key not found. Existing keys: ${keys.toList()}" }
    val value = this.getValue(key)
    check(value is T) { "Unexpected type in config" }
    return value
}
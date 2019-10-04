package org.jb.cce.util

import com.google.gson.*
import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterManager
import org.jb.cce.uast.Language
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object ConfigFactory {
    private val gson = GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapter(EvaluationFilter::class.java, object : JsonSerializer<EvaluationFilter> {
                override fun serialize(src: EvaluationFilter, typeOfSrc: Type, context: JsonSerializationContext) = src.toJson()
            })
            .create()

    private val defaultConfig = Config("", listOf(""), Language.JAVA.displayName,
            CompletionStrategy(CompletionPrefix.NoPrefix, CompletionContext.ALL, false,
                    EvaluationFilterManager.getAllFilters().associateBy({ it.id }, { it.defaultFilter() })),
            CompletionType.BASIC, "", interpretActions = true, saveLogs = false, logsTrainingPercentage = 70)

    fun load(path: String): Config {
        val configFile = File(path)
        if (!configFile.exists()) {
            save(defaultConfig, path)
            throw IllegalArgumentException("Config file missing. Config created by path: ${configFile.absolutePath}. Fill settings in config.")
        }

        val map = gson.fromJson<HashMap<String, Any>>(FileReader(configFile), HashMap<String, Any>().javaClass)
        val strategy = map.getAs<Map<String, Any>>("strategy")
        val evaluationFilters = mutableMapOf<String, EvaluationFilter>()
        val languageName = map.getAs<String>("language")
        val completeAllTokens = strategy.getAs<Boolean>("completeAllTokens")
        if (!completeAllTokens) {
            val filters = strategy.getAs<Map<String, Any>>("filters")
            for ((id, description) in filters) {
                val configuration = EvaluationFilterManager.getConfigurationById(id)
                        ?: throw IllegalStateException("Unknown filter: $id")
                assert(configuration.isLanguageSupported(languageName)) { "filter $id is not supported for this language" }
                evaluationFilters[id] = configuration.buildFromJson(description)
            }
        }
        return Config(map.getAs("projectPath"), map.getAs("listOfFiles"), languageName,
                CompletionStrategy(getPrefix(strategy), CompletionContext.valueOf(strategy.getAs("context")), completeAllTokens, evaluationFilters),
                CompletionType.valueOf(map.getAs("completionType")), map.getAs("outputDir"), map.getAs("interpretActions"),
                map.getAs("saveLogs"), map.getAs<Double>("logsTrainingPercentage").toInt())
    }

    fun save(path: String): Config {
        save(defaultConfig, path)
        return defaultConfig
    }

    fun save(config: Config, path: String) {
        val json = gson.toJsonTree(config)
        val strategy = json.asJsonObject["strategy"] as JsonObject
        val prefix = strategy["prefix"].asJsonObject
        val className = config.strategy.prefix.javaClass.name
        prefix.add("name", JsonPrimitive(className.substring(className.indexOf('$') + 1)))
        val strategyPrefix = config.strategy.prefix
        if (strategyPrefix !is CompletionPrefix.NoPrefix) prefix.add("emulateTyping", JsonPrimitive(strategyPrefix.emulateTyping))
        if (strategyPrefix is CompletionPrefix.SimplePrefix) prefix.add("n", JsonPrimitive(strategyPrefix.n))
        Files.write(Paths.get(path), gson.toJson(json).toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
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

    private inline fun <reified T> Map<String, *>.getAs(key: String): T {
        check(key in this.keys) { "$key not found. Existing keys: ${keys.toList()}" }
        val value = this.getValue(key)
        check(value is T) { "Unexpected type in config" }
        return value
    }
}
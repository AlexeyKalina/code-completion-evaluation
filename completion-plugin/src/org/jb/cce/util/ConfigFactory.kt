package org.jb.cce.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterManager
import org.jb.cce.uast.Language
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object ConfigFactory {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val defaultConfig = Config("", listOf(""), Language.JAVA.displayName,
            CompletionStrategy(CompletionPrefix.NoPrefix, CompletionContext.ALL, false, emptyList()),
            CompletionType.BASIC, "", interpretActions = false, saveLogs = true, logsTrainingPercentage = 70)

    fun load(path: String): Config {
        val configFile = File(path)
        if (!configFile.exists()) {
            save(defaultConfig, path)
            throw IllegalArgumentException("Config file missing. Config created by path: ${configFile.absolutePath}. Fill settings in config.")
        }

        val map = gson.fromJson(FileReader(configFile), HashMap<String, Any>().javaClass)
        val strategy = map["strategy"] as Map<String, Any>
        val filters = strategy["filters"] as Map<String, Any>
        val evaluationFilters = mutableListOf<EvaluationFilter>()
        for ((id, description) in filters) {
            val configuration = EvaluationFilterManager.getConfigurationById(id)
                ?: throw IllegalStateException("Unexpected filter: $id")
//            assert(configuration.supportedLanguages().contains("language from config.json")) { "filter $id is not supported for this lagnuages" }
            // TODO: fix it
            evaluationFilters.add(configuration.buildFromJson(description))
        }
        return Config(map["projectPath"] as String, map["listOfFiles"] as List<String>, map["language"] as String,
                CompletionStrategy(getPrefix(strategy), CompletionContext.valueOf(strategy["context"] as String),
                        map["completeAllTokens"] as Boolean, evaluationFilters),
                CompletionType.valueOf(map["completionType"] as String), map["outputDir"] as String, map["interpretActions"] as Boolean,
                map["saveLogs"] as Boolean, (map["logsTrainingPercentage"] as Double).toInt())
    }

    fun save(config: Config, path: String) {
        val json = gson.toJsonTree(config)
        val prefix = json.asJsonObject["strategy"].asJsonObject["prefix"].asJsonObject
        val className = config.strategy.prefix.javaClass.name
        prefix.add("name", JsonPrimitive(className.substring(className.indexOf('$') + 1)))
        val strategyPrefix = config.strategy.prefix
        if (strategyPrefix !is CompletionPrefix.NoPrefix) prefix.add("emulateTyping", JsonPrimitive(strategyPrefix.emulateTyping))
        if (strategyPrefix is CompletionPrefix.SimplePrefix) prefix.add("n", JsonPrimitive(strategyPrefix.n))
        Files.write(Paths.get(path), gson.toJson(json).toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    private fun getPrefix(strategy: Map<String, Any>): CompletionPrefix {
        val prefix = strategy["prefix"] as Map<String, Any>
        return when (prefix["name"]) {
            "NoPrefix" -> CompletionPrefix.NoPrefix
            "CapitalizePrefix" -> CompletionPrefix.CapitalizePrefix(prefix["emulateTyping"] as Boolean)
            "SimplePrefix" -> CompletionPrefix.SimplePrefix(prefix["emulateTyping"] as Boolean, (prefix["n"] as Double).toInt())
            else -> throw IllegalArgumentException("Unknown completion prefix")
        }
    }
}
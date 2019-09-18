package org.jb.cce.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import org.jb.cce.actions.*
import org.jb.cce.uast.Language
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object ConfigFactory {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val defaultConfig = Config("", listOf(""), Language.JAVA.displayName,
            CompletionStrategy(CompletionPrefix.NoPrefix, CompletionContext.ALL, false, Filters(listOf(TypeFilter.METHOD_CALLS), ArgumentFilter.ALL, StaticFilter.ALL, "")),
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
        return Config(map["projectPath"] as String, map["listOfFiles"] as List<String>, map["language"] as String,
                CompletionStrategy(getPrefix(strategy), CompletionContext.valueOf(strategy["context"] as String),
                        map["completeAllTokens"] as Boolean, Filters((filters["typeFilters"] as List<String>).map { TypeFilter.valueOf(it) },
                        ArgumentFilter.valueOf(filters["argumentFilter"] as String), StaticFilter.valueOf(filters["staticFilter"] as String),
                        filters["packagePrefix"] as String)),
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
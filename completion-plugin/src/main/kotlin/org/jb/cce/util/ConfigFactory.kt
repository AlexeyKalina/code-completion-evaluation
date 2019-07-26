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

    private val defaultConfig = Config("", listOf(""), Language.JAVA,
            CompletionStrategy(CompletionPrefix.NoPrefix, CompletionStatement.METHOD_CALLS, CompletionContext.ALL),
            listOf(CompletionType.BASIC), "")

    fun load(path: String): Config {
        val configFile = File(path)
        if (!configFile.exists()) {
            save(defaultConfig, path)
            throw IllegalArgumentException("Config file missing. Config created by path: ${configFile.absolutePath}. Fill settings in config.")
        }

        val map = gson.fromJson(FileReader(configFile), HashMap<String, Any>().javaClass)
        val strategy = map["strategy"] as Map<String, Any>
        return Config(map["projectPath"] as String, map["listOfFiles"] as List<String>, Language.valueOf(map["language"] as String),
                CompletionStrategy(getPrefix(strategy), CompletionStatement.valueOf(strategy["statement"] as String),
                        CompletionContext.valueOf(strategy["context"] as String)),
                (map["completionTypes"] as List<String>).map { CompletionType.valueOf(it) }, map["outputDir"] as String)
    }

    fun save(config: Config, path: String) {
        val json = gson.toJsonTree(config)
        val prefix = json.asJsonObject["strategy"].asJsonObject["prefix"].asJsonObject
        prefix.add("name", JsonPrimitive(config.strategy.prefix.javaClass.name))
        val strategyPrefix = config.strategy.prefix
        if (strategyPrefix is CompletionPrefix.SimplePrefix) prefix.add("n", JsonPrimitive(strategyPrefix.n))
        Files.write(Paths.get(path), gson.toJson(json).toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    private fun getPrefix(strategy: Map<String, Any>): CompletionPrefix {
        val prefix = strategy["prefix"] as Map<String, Any>
        return when (prefix["name"]) {
            CompletionPrefix.NoPrefix::class.java.name -> CompletionPrefix.NoPrefix
            CompletionPrefix.CapitalizePrefix::class.java.name -> CompletionPrefix.CapitalizePrefix
            CompletionPrefix.SimplePrefix::class.java.name -> CompletionPrefix.SimplePrefix((prefix["n"] as Double).toInt())
            else -> throw IllegalArgumentException("Unknown completion prefix")
        }
    }
}
package org.jb.cce.util

import com.google.gson.GsonBuilder
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import org.jb.cce.actions.*
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ConfigFactory {
    private val gson = GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapter(CompletionStrategy::class.java, CompletionStrategySerializer())
            .create()

    fun defaultConfig(projectPath: String = "", language: String = "Java") = Config.build(projectPath, language) {}

    fun load(path: Path): Config {
        val configFile = path.toFile()
        if (!configFile.exists()) {
            save(defaultConfig(), configFile.parent, configFile.name)
            throw IllegalArgumentException("Config file missing. Config created by path: ${configFile.absolutePath}. Fill settings in config.")
        }

        return deserialize(configFile.readText())
    }

    fun save(config: Config, directoryPath: String, name: String = "config.json") {
        val json = serialize(config)
        Files.write(Paths.get(directoryPath, name), json.toByteArray())
    }

    fun getByKey(project: Project, configStateKey: String): Config {
        val properties = PropertiesComponent.getInstance(project)
        val configState = properties.getValue(configStateKey) ?: return defaultConfig(project.basePath!!)
        return deserialize(configState)
    }

    fun storeByKey(project: Project, configStateKey: String, config: Config) {
        val properties = PropertiesComponent.getInstance(project)
        properties.setValue(configStateKey, serialize(config))
    }

    private fun deserialize(json: String): Config {
        val map = gson.fromJson<HashMap<String, Any>>(json, HashMap<String, Any>().javaClass)
        val languageName = map.getAs<String>("language")
        val builder = Config.Builder(map.getAs("projectPath"), languageName)
        val strategyJson = map.getAs<Map<String, Any>>("strategy")
        CompletionStrategyDeserializer().deserialize(strategyJson, languageName, builder)
        builder.evaluationRoots = map.getAs("listOfFiles")
        builder.completionType = CompletionType.valueOf(map.getAs("completionType"))
        builder.workspaceDir = map.getAs("workspaceDir")
        builder.interpretActions = map.getAs("interpretActions")
        builder.saveLogs = map.getAs("saveLogs")
        builder.trainTestSplit = map.getAs<Double>("trainTestSplit").toInt()
        return builder.build()
    }

    private fun serialize(config: Config): String = gson.toJson(config)

    private class CompletionStrategyDeserializer {
        fun deserialize(strategy: Map<String, Any>, language: String, builder: Config.Builder) {
            val completeAllTokens = strategy.getAs<Boolean>("completeAllTokens")
            builder.allTokens = completeAllTokens
            if (!completeAllTokens) {
                val evaluationFilters = mutableMapOf<String, EvaluationFilter>()
                val filters = strategy.getAs<Map<String, Any>>("filters")
                for ((id, description) in filters) {
                    val configuration = EvaluationFilterManager.getConfigurationById(id)
                            ?: throw IllegalStateException("Unknown filter: $id")
                    assert(configuration.isLanguageSupported(language)) { "filter $id is not supported for this language" }
                    evaluationFilters[id] = configuration.buildFromJson(description)
                }
                builder.filters = evaluationFilters
            }
            builder.prefixStrategy = getPrefix(strategy)
            builder.contextStrategy = CompletionContext.valueOf(strategy.getAs("context"))
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

    private inline fun <reified T> Map<String, *>.getAs(key: String): T {
        check(key in this.keys) { "$key not found. Existing keys: ${keys.toList()}" }
        val value = this.getValue(key)
        check(value is T) { "Unexpected type in config" }
        return value
    }
}
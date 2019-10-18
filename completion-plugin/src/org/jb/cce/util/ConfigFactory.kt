package org.jb.cce.util

import com.google.gson.GsonBuilder
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import org.jb.cce.actions.*
import org.jb.cce.filter.EvaluationFilterManager
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths

object ConfigFactory {
    private val gson = GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .registerTypeAdapter(CompletionStrategy::class.java, CompletionStrategySerializer())
            .create()

    private fun defaultConfig(projectPath: String = "", language: String = "Java") = Config.build(projectPath, language) {}

    fun load(path: String): Config {
        val configFile = File(path)
        if (!configFile.exists()) {
            save(defaultConfig(), path)
            throw IllegalArgumentException("Config file missing. Config created by path: ${configFile.absolutePath}. Fill settings in config.")
        }

        return deserialize(FileReader(configFile).readText())
    }

    fun save(path: String): Config {
        save(defaultConfig(), path)
        return defaultConfig()
    }

    fun save(config: Config, path: String) {
        val json = serialize(config)
        Files.write(Paths.get(path), json.toByteArray())
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
        val strategyJson = map.getAs<Map<String, Any>>("strategy")
        val strategy = CompletionStrategyDeserializer().deserialize(strategyJson)
        val languageName = map.getAs<String>("language")
        for ((id, _) in strategy.filters) {
            val configuration = EvaluationFilterManager.getConfigurationById(id)
            assert(configuration!!.isLanguageSupported(languageName)) { "filter $id is not supported for this language" }
        }
        return Config(map.getAs("projectPath"), map.getAs("listOfFiles"), languageName,
                strategy,
                CompletionType.valueOf(map.getAs("completionType")), map.getAs("workspaceDir"), map.getAs("interpretActions"),
                map.getAs("saveLogs"), map.getAs<Double>("trainTestSplit").toInt())
    }

    private fun serialize(config: Config): String = gson.toJson(config)
}
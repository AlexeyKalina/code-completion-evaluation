package org.jb.cce

import com.google.gson.Gson
import org.jb.cce.storages.ActionsStorage
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.LogsStorage
import org.jb.cce.storages.SessionsStorage
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class EvaluationWorkspace private constructor(private val basePath: Path) {
    companion object {
        private const val STATS_FILE_NAME = "stats.json"
        private const val DEFAULT_REPORT_TYPE = "html"
        private val gson = Gson()
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

        fun open(workspaceDir: String): EvaluationWorkspace {
            return EvaluationWorkspace(Paths.get(workspaceDir).toAbsolutePath())
        }

        fun create(config: Config): EvaluationWorkspace {
            val workspace = EvaluationWorkspace(Paths.get(config.outputDir).toAbsolutePath().resolve(formatter.format(Date())))
            workspace.writeConfig(config)
            return workspace
        }
    }

    private val sessionsDir = subdir("data")
    private val logsDir = subdir("logs")
    private val actionsDir = subdir("actions")
    private val errorsDir = subdir("errors")
    private val reportsDir = subdir("reports")
    private val pathToConfig = path().resolve(ConfigFactory.DEFAULT_CONFIG_NAME)
    private val _reports: MutableMap<String, MutableMap<String, Path>> = mutableMapOf()

    val sessionsStorage: SessionsStorage = SessionsStorage(sessionsDir.toString())

    val actionsStorage: ActionsStorage = ActionsStorage(actionsDir.toString())

    val errorsStorage: FileErrorsStorage = FileErrorsStorage(errorsDir.toString())

    val logsStorage: LogsStorage = LogsStorage(logsDir.toString())

    override fun toString(): String = "Evaluation workspace: $basePath"

    fun reportsDirectory(): String = reportsDir.toString()

    fun path(): Path = basePath

    fun readConfig(): Config = ConfigFactory.load(pathToConfig)

    fun dumpStatistics(stats: Map<String, Long>) =
            FileWriter(basePath.resolve(STATS_FILE_NAME).toString()).use { it.write(gson.toJson(stats)) }

    fun addReport(reportType: String, filterName: String, reportPath: Path) {
        _reports.getOrPut(reportType) { mutableMapOf() }[filterName] = reportPath
    }

    fun getReports(reportType: String = DEFAULT_REPORT_TYPE): Map<String, Path> = _reports.getOrDefault(reportType, emptyMap())

    private fun writeConfig(config: Config) = ConfigFactory.save(config, basePath)

    private fun subdir(name: String): Path {
        val directory = basePath.resolve(name)
        Files.createDirectories(directory)
        return directory
    }
}
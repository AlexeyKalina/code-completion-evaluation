package org.jb.cce.evaluation.step

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.PlatformComponentManagerImpl
import com.intellij.stats.sender.StatisticSender
import com.intellij.stats.storage.FilePathProvider
import com.intellij.stats.storage.UniqueFilesProvider
import org.jb.cce.EvaluationWorkspace
import java.io.File
import java.nio.file.Paths

class SetupStatsCollectorStep(private val project: Project) : EvaluationStep {
    companion object {
        private const val statsCollectorId = "com.intellij.stats.completion"
        fun statsCollectorLogsDirectory(): String = Paths.get(PathManager.getSystemPath(), "completion-stats-data").toString()
    }

    override val name: String = "Setup Stats Collector step"
    override val description: String = "Configure plugin Stats Collector if needed"

    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val statsCollectorEnabled = PluginManager.getPlugin(PluginId.getId(statsCollectorId))?.isEnabled ?: false
        if (!statsCollectorEnabled) {
            println("Stats Collector plugin isn't installed. Install it, if you want to save completion logs.")
            return null
        }

        File(statsCollectorLogsDirectory()).deleteRecursively()
        val serviceManager = ApplicationManager.getApplication() as PlatformComponentManagerImpl
        val filesProvider = object : UniqueFilesProvider("chunk", PathManager.getSystemPath(), "completion-stats-data") {
            override fun cleanupOldFiles() = Unit
        }
        val statisticSender = object : StatisticSender {
            override fun sendStatsData(url: String) = Unit
        }
        serviceManager.replaceServiceInstance(FilePathProvider::class.java, filesProvider, project)
        serviceManager.replaceServiceInstance(StatisticSender::class.java, statisticSender, project)
        return workspace
    }
}
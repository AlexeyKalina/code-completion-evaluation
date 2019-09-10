package org.jb.cce.actions

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.CompletionEvaluator
import org.jb.cce.util.ConfigFactory
import java.nio.file.Paths

class CompletionEvaluationStarter : ApplicationStarter {
    override fun getCommandName(): String = "evaluate-completion"
    override fun isHeadless(): Boolean = true

    override fun main(params: Array<out String>) {
        val configPath = if (params.size == 2) params[1] else "config.json"
        println("Config path: ${Paths.get(configPath).toAbsolutePath()}")

        val config = try {
            ConfigFactory.load(configPath)
        } catch (e: Exception) {
            println("Error for loading config: $configPath, $e")
            return
        }
        val project = ProjectUtil.openProject(config.projectPath, null, false)
        if (project == null) {
            println("Project ${config.projectPath} not found.")
            return
        }
        val projectBasePath = project.basePath
        if (projectBasePath == null) {
            println("Evaluation for default project impossible. Path: ${config.projectPath}")
            return
        }
        val files = config.listOfFiles.findFilesInProject(projectBasePath)

        CompletionEvaluator(true).evaluateCompletion(project, files, config.language, config.strategy,
                config.completionType, config.outputDir, config.interpretActions, config.saveLogs, config.logsTrainingPercentage)
    }

    private fun List<String>.findFilesInProject(projectRootPath: String): List<VirtualFile> {
        val fileSystem = LocalFileSystem.getInstance()
        val projectRoot = Paths.get(projectRootPath)
        val path2file = this.associateWith { fileSystem.findFileByIoFile(projectRoot.resolve(it).toFile()) }
        val unknownFiles = path2file.filterValues { it == null }
        require(unknownFiles.isEmpty()) { "Evaluation roots not found: ${unknownFiles.keys}" }

        return path2file.values.filterNotNull()
    }
}

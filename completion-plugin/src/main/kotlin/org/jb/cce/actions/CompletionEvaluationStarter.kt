package org.jb.cce.actions

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.vfs.LocalFileSystem
import org.jb.cce.CompletionEvaluator
import org.jb.cce.util.ConfigFactory
import java.io.File
import java.nio.file.Paths

class CompletionEvaluationStarter : ApplicationStarter {
    override fun getCommandName(): String = "evaluate-completion"
    override fun isHeadless(): Boolean = true
    override fun canProcessExternalCommandLine(): Boolean = true

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
        val fileSystem = LocalFileSystem.getInstance()
        val files  = config.listOfFiles.map { fileSystem.findFileByIoFile(File(it))!! }

        CompletionEvaluator(true).evaluateCompletion(project, files, config.language, config.strategy,
                config.completionTypes, config.outputDir)
    }
}
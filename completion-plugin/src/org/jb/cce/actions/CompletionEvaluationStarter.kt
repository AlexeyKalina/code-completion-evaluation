package org.jb.cce.actions

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.jb.cce.CompletionEvaluator
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import org.jb.cce.util.ConfigFactory
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

class CompletionEvaluationStarter : ApplicationStarter {
    override fun getCommandName(): String = "evaluate-completion"
    override fun isHeadless(): Boolean = true

    override fun main(params: Array<out String>) {
        val configPath = if (params.size == 2) params[1] else "config.json"
        println("Config path: ${Paths.get(configPath).toAbsolutePath()}")

        val config = try {
            ConfigFactory.load(configPath)
        } catch (e: Exception) {
            fatalError("Error for loading config: $configPath, $e. StackTrace: ${stackTraceToString(e)}")
        }

        val project = try {
            openProjectHeadless(config.projectPath)
        }
        catch (e: Throwable) {
            fatalError("Project could not be loaded: $e")
        }

        try {
            CompletionEvaluator(true, project).evaluateCompletion(config)
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            fatalError("Could not start evaluation: ${e.message}")
        }
    }

    private fun fatalError(msg: String): Nothing {
        System.err.println("Evaluation failed: $msg")
        exitProcess(1)
    }

    private fun openProjectHeadless(projectPath: String): Project {
        val project = File(projectPath)

        assert (project.exists()) { "File $projectPath does not exist" }
        assert (project.isDirectory) { "$projectPath is not a directory" }

        val projectDir = File(project, Project.DIRECTORY_STORE_FOLDER)
        assert(projectDir.exists()) { "$projectPath is not a project. .idea directory is missing" }

        val existing = ProjectManager.getInstance().openProjects.firstOrNull { proj ->
            !proj.isDefault && ProjectUtil.isSameProject(projectPath, proj)
        }
        if (existing != null) return existing

        return ProjectManager.getInstance().loadAndOpenProject(projectPath)!!
    }
}

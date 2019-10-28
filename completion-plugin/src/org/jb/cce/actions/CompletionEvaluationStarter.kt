package org.jb.cce.actions

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.jb.cce.*
import org.jb.cce.evaluation.ActionsGenerationEvaluator
import org.jb.cce.evaluation.ActionsInterpretationEvaluator
import org.jb.cce.evaluation.ReportGenerationEvaluator
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import org.jb.cce.util.Config
import org.jb.cce.util.ConfigFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

class CompletionEvaluationStarter : ApplicationStarter {
    override fun getCommandName(): String = "evaluate-completion"
    override fun isHeadless(): Boolean = true

    override fun main(params: Array<out String>) {
        val paramsList = params.toMutableList()
        paramsList.removeAt(0)
        val interpret = paramsList.remove("--interpret-actions") || paramsList.remove("-i")
        val generateReport = paramsList.remove("--generate-report") || paramsList.remove("-r")

        when {
            interpret -> {
                if (paramsList.size != 1) fatalError("Unexpected arguments count")
                interpretActions(generateReport, paramsList.first())
            }
            generateReport -> {
                if (paramsList.size == 0) fatalError("Unexpected arguments count")
                generateReport(paramsList)
            }
            else -> {
                val configPath =
                        when {
                            paramsList.isEmpty() -> ConfigFactory.DEFAULT_CONFIG_NAME
                            paramsList.size == 1 -> paramsList.first()
                            else -> fatalError("Unexpected arguments count")
                        }
                val path = Paths.get(configPath).toAbsolutePath()
                println("Config path: $path")
                evaluateCompletion(path)
            }
        }
    }

    private fun evaluateCompletion(configPath: Path) {
        val config = loadConfig(configPath)
        val project = loadProject(config.projectPath)
        ActionsGenerationEvaluator(project, true).evaluateUnderProgress(config, null, null)
    }

    private fun interpretActions(generateReport: Boolean, workspacePath: String) {
        val configPath = Paths.get(workspacePath, ConfigFactory.DEFAULT_CONFIG_NAME)
        val config = loadConfig(configPath)
        val workspace = EvaluationWorkspace(workspacePath, config.completionType, existing = true)
        val project = loadProject(config.projectPath)
        val evaluator = ActionsInterpretationEvaluator(project, true)
        evaluator.evaluateUnderProgress(workspace, config, true, generateReport, false)
    }

    private fun generateReport(paths: List<String>) {
        val workspaces = mutableListOf<EvaluationWorkspace>()

        lateinit var config: Config
        for (workspacePath in paths) {
            config = ConfigFactory.load(Paths.get(workspacePath, ConfigFactory.DEFAULT_CONFIG_NAME))
            workspaces.add(EvaluationWorkspace(workspacePath, config.completionType, true).apply {
                sessionsStorage.evaluationTitle = config.evaluationTitle
            })
        }

        val project = loadProject(config.projectPath)
        val workspace = EvaluationWorkspace(config.outputDir, config.completionType)
        val reportGenerator = HtmlReportGenerator(workspace.reportsDirectory())
        val evaluator = ReportGenerationEvaluator(reportGenerator, project, true)
        evaluator.generateReportUnderProgress(workspaces.map { it.sessionsStorage }, workspaces.map { it.errorsStorage })
    }

    private fun loadConfig(configPath: Path) = try {
        ConfigFactory.load(configPath)
    } catch (e: Exception) {
        fatalError("Error for loading config: $configPath, $e. StackTrace: ${stackTraceToString(e)}")
    }

    private fun loadProject(projectPath: String) = try {
        openProjectHeadless(projectPath)
    } catch (e: Throwable) {
        fatalError("Project could not be loaded: $e")
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

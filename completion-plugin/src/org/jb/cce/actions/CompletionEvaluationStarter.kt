package org.jb.cce.actions

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.jb.cce.ConfigFactory
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

class CompletionEvaluationStarter : ApplicationStarter {
    override fun getCommandName(): String = "evaluate-completion"
    override fun isHeadless(): Boolean = true

    override fun main(args: Array<String>) =
            MainEvaluationCommand()
                    .subcommands(FullCommand(), CustomCommand(), MultipleEvaluations(), CompareEvaluations())
                    .main(args.toList().subList(1, args.size))

    abstract class EvaluationCommand(name: String, help: String): CliktCommand(name = name, help = help) {
        protected fun loadConfig(configPath: Path) = try {
            ConfigFactory.load(configPath)
        } catch (e: Exception) {
            fatalError("Error for loading config: $configPath, $e. StackTrace: ${stackTraceToString(e)}")
        }

        protected fun loadProject(projectPath: String): Project = try {
            println("Open and load project $projectPath. Operation may take few minutes.")
            val project = openProjectHeadless(projectPath)
            println("Project loaded!")
            project
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

    inner class MainEvaluationCommand: EvaluationCommand(commandName, "Evaluate code completion quality in headless mode") {
        override fun run() = Unit
    }

    class FullCommand: EvaluationCommand(name = "full", help = "Start process from actions generation (set up by config)") {
        private val configPath by argument(name = "config-path", help = "Path to config").default(ConfigFactory.DEFAULT_CONFIG_NAME)

        override fun run() {
            val config = loadConfig(Paths.get(configPath))
            val workspace = EvaluationWorkspace.create(config)
            val project = loadProject(config.projectPath)
            val process = EvaluationProcess.build({
                shouldGenerateActions = true
                shouldInterpretActions = config.interpretActions
                shouldGenerateReports = config.interpretActions
            }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))
            process.startAsync(workspace)
        }
    }

    class CustomCommand: EvaluationCommand(name = "custom", help = "Start process from actions interpretation or report generation") {
        private val workspacePath by argument(name = "workspace", help = "Path to workspace")
        private val interpretActions by option(names = *arrayOf("--interpret-actions", "-i"), help = "Interpret actions").flag()
        private val generateReport by option(names = *arrayOf("--generate-report", "-r"), help = "Generate report").flag()

        override fun run() {
            val workspace = EvaluationWorkspace.open(workspacePath)
            val config = workspace.readConfig()
            val project = loadProject(config.projectPath)
            val process = EvaluationProcess.build({
                shouldGenerateActions = false
                shouldInterpretActions = interpretActions
                shouldGenerateReports = generateReport
            }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))
            process.startAsync(workspace)
        }
    }

    abstract class MultipleEvaluationsBase(name: String, help: String) : EvaluationCommand(name, help) {
        abstract val workspaces: List<String>

        override fun run() {
            val workspacePath = Paths.get(workspaces.first())
            val existingWorkspace = EvaluationWorkspace.open(workspacePath.toString())
            val config = existingWorkspace.readConfig()
            val project = loadProject(config.projectPath)
            val outputWorkspace = EvaluationWorkspace.create(config)
            val process = EvaluationProcess.build({
                shouldGenerateReports = true
            }, BackgroundStepFactory(config, project, true, workspaces, EvaluationRootInfo(true)))
            process.startAsync(outputWorkspace)
        }
    }

    class MultipleEvaluations : MultipleEvaluationsBase(name = "multiple-evaluations", help = "Generate report by multiple evaluations") {
        override val workspaces by argument(name = "workspaces", help = "List of workspaces").multiple()
    }

    class CompareEvaluations : MultipleEvaluationsBase(name = "compare-in", help = "Generate report for all evaluation workspaces in a directory") {
        private val root by argument(name = "directory", help = "Root directory for evaluation workspaces")

        override val workspaces: List<String>
            get() {
                val outputDirectory = Paths.get(root)
                if (!outputDirectory.exists() || !outputDirectory.isDirectory()) {
                    throw BadParameterValue("Directory \"$root\" not found.")
                }

                val nestedFiles = outputDirectory.toFile().listFiles() ?: emptyArray()

                val result = nestedFiles.filter { it.isDirectory }.map { it.absolutePath }
                if (result.isEmpty()) {
                    throw BadParameterValue("Directory \"$root\" should not be empty")
                }

                return result
            }
    }
}

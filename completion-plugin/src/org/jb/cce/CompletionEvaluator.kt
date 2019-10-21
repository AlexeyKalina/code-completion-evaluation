package org.jb.cce

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import org.jb.cce.actions.ActionsGenerator
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import org.jb.cce.highlighter.Highlighter
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileSessionsInfo
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.interpretator.InterpretationHandlerImpl
import org.jb.cce.util.*
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.jb.cce.visitors.EvaluationRootByOffsetVisitor
import org.jb.cce.visitors.EvaluationRootByRangeVisitor
import java.nio.file.Paths
import java.util.*
import kotlin.system.measureTimeMillis

class CompletionEvaluator(private val isHeadless: Boolean, private val project: Project) {
    private companion object {
        const val tempDirName = "code-completion-evaluation"
        val LOG = Logger.getInstance(CompletionEvaluator::class.java)
    }

    fun evaluateCompletion(config: Config) = evaluateUnderProgress(config, null, null)

    fun evaluateCompletionHere(config: Config, offset: Int, psi: PsiElement?) = evaluateUnderProgress(config, offset, psi)

    private fun evaluateUnderProgress(config: Config, offset: Int?, psi: PsiElement?) {
        val task = object : Task.Backgroundable(project, "Generating actions", true) {
            private val workspace = EvaluationWorkspace(config.workspaceDir, config.completionType.name)

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                ConfigFactory.save(config, workspace.toString())
                generateActions(workspace, config.language, config.listOfFiles, config.strategy, offset, psi, getProcess(indicator))
            }

            override fun onSuccess() {
                if (config.interpretActions)
                    interpretUnderProgress(workspace, config.completionType, config.strategy, config.language,
                            offset == null, config.saveLogs, config.trainTestSplit)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(workspace: EvaluationWorkspace, languageName: String, files: Collection<String>,
                                strategy: CompletionStrategy, offset: Int?, psi: PsiElement?, indicator: Progress) {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, languageName, strategy.completeAllTokens)

        val errors = mutableListOf<FileErrorInfo>()
        var completed = 0
        for (filePath in files) {
            val file = FilesHelper.getFile(project, filePath)
            if (indicator.isCanceled()) {
                LOG.info("Generating actions is canceled by user. Done: $completed/${files.size}. With error: ${errors.size}")
                break
            }
            LOG.info("Start generating actions for file ${file.path}. Done: $completed/${files.size}. With error: ${errors.size}")
            indicator.setProgress(file.name, file.name, completed.toDouble() / files.size)
            try {
                val rootVisitor = when {
                    psi != null -> EvaluationRootByRangeVisitor(psi.textRange?.startOffset ?: psi.textOffset,
                            psi.textRange?.endOffset ?:psi.textOffset + psi.textLength)
                    offset != null -> EvaluationRootByOffsetVisitor(offset, file.path, file.text())
                    else -> DefaultEvaluationRootVisitor()
                }
                val uast = uastBuilder.build(file, rootVisitor)
                val fileActions = actionsGenerator.generate(uast)
                workspace.actionsStorage.saveActions(fileActions)
            } catch (e: Throwable) {
                workspace.errorsStorage.saveError(FileErrorInfo(FilesHelper.getRelativeToProjectPath(project, file.path), e.message ?: "No Message", stackTraceToString(e)))
                LOG.error("Generating actions error for file ${file.path}.", e)
            }
            completed++
            LOG.info("Generating actions for file ${file.path} completed. Done: $completed/${files.size}. With error: ${errors.size}")
        }
    }

    private fun interpretUnderProgress(workspace: EvaluationWorkspace, completionType: CompletionType, strategy: CompletionStrategy,
                                       languageName: String, generateReport: Boolean, saveLogs: Boolean, logsTrainingPercentage: Int) {
        val task = object : Task.Backgroundable(project, "Actions interpreting") {
            private lateinit var lastFileSessions: List<Session>

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                if (saveLogs) workspace.logsStorage.watch(statsCollectorLogsDirectory(), languageName, logsTrainingPercentage)
                lastFileSessions = interpretActions(workspace, completionType, strategy, project, getProcess(indicator))
            }

            override fun onSuccess() = finish()
            override fun onCancel() = finish()

            private fun finish() {
                if (saveLogs) workspace.logsStorage.stopWatching()
                if (!generateReport) return Highlighter(project).highlight(lastFileSessions)
                val reportGenerator = HtmlReportGenerator(workspace.reportsDirectory())
                ReportGeneration(reportGenerator).generateReportUnderProgress(listOf(workspace.sessionsStorage), listOf(workspace.errorsStorage), project, isHeadless)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(workspace: EvaluationWorkspace, completionType: CompletionType, strategy: CompletionStrategy,
                                 project: Project, indicator: Progress): List<Session> {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project, completionType), project)
        var sessionsCount = 0
        val computingTime = measureTimeMillis {
            sessionsCount = workspace.actionsStorage.computeSessionsCount()
        }
        LOG.info("Computing of sessions count took $computingTime ms")
        val handler = InterpretationHandlerImpl(indicator, sessionsCount)
        val interpreter = Interpreter(completionInvoker, handler, project.basePath)
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        setMLCompletion(completionType == CompletionType.ML)
        val files = workspace.actionsStorage.getActionFiles()
        var lastFileSessions = listOf<Session>()
        for (file in files) {
            val fileActions = workspace.actionsStorage.getActions(file)
            try {
                lastFileSessions = interpreter.interpret(fileActions)
                val fileText = FilesHelper.getFile(project, fileActions.path).text()
                workspace.sessionsStorage.saveSessions(FileSessionsInfo(fileActions.path, fileText, lastFileSessions))
            } catch (e: Throwable) {
                workspace.errorsStorage.saveError(FileErrorInfo(fileActions.path, e.message ?: "No Message", stackTraceToString(e)))
                LOG.error("Actions interpretation error for file $file.", e)
            }
            if (handler.isCancelled()) break
        }
        workspace.sessionsStorage.saveEvaluationInfo(completionType.name)
        LOG.info("Interpreting actions completed")
        setMLCompletion(mlCompletionFlag)
        return lastFileSessions
    }

    private fun getProcess(indicator: ProgressIndicator) = if (isHeadless) CommandLineProgress(indicator.text) else IdeaProgress(indicator)

    private fun statsCollectorLogsDirectory(): String {
        return Paths.get(PathManager.getSystemPath(), "completion-stats-data").toString()
    }

    private fun isMLCompletionEnabled(): Boolean {
        return try {
            Registry.get(PropertKey@ "completion.stats.enable.ml.ranking").asBoolean()
        } catch (e: MissingResourceException) {
            false
        }
    }
    private fun setMLCompletion(value: Boolean) {
        try {
            Registry.get(PropertKey@ "completion.stats.enable.ml.ranking").setValue(value)
        } catch (e: MissingResourceException) {
        }
    }
}
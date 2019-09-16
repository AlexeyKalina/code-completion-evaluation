package org.jb.cce

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.io.readText
import org.jb.cce.actions.ActionsGenerator
import org.jb.cce.actions.CompletionStatement
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.highlighter.Highlighter
import org.jb.cce.info.EvaluationInfo
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileSessionsInfo
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.interpretator.InterpretationHandlerImpl
import org.jb.cce.util.*
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.jb.cce.visitors.EvaluationRootByOffsetVisitor
import org.jb.cce.visitors.EvaluationRootByRangeVisitor
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Paths
import java.util.*
import kotlin.system.measureTimeMillis

class CompletionEvaluator(private val isHeadless: Boolean, private val project: Project) {
    private companion object {
        val LOG = Logger.getInstance(CompletionEvaluator::class.java)
    }

    fun evaluateCompletion(files: List<VirtualFile>, languageName: String, strategy: CompletionStrategy,
                           completionType: CompletionType, workspaceDir: String, interpretActions: Boolean, saveLogs: Boolean, logsTrainingPercentage: Int) {
        val language2files = FilesHelper.getFiles(project, files)
        if (language2files.isEmpty()) {
            println("Languages of selected files aren't supported.")
            return finishWork(null, project, isHeadless)
        }
        evaluateUnderProgress(languageName, language2files.getValue(languageName), strategy, completionType, workspaceDir, interpretActions, saveLogs, logsTrainingPercentage, null, null)
    }

    fun evaluateCompletionHere(file: VirtualFile, languageName: String, offset: Int, psi: PsiElement?,
                               strategy: CompletionStrategy, completionType: CompletionType) =
            evaluateUnderProgress(languageName, listOf(file), strategy, completionType, "", true, false, 0, offset, psi)

    private fun evaluateUnderProgress(languageName: String, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                      completionType: CompletionType, workspaceDir: String, interpretActions: Boolean, saveLogs: Boolean,
                                      logsTrainingPercentage: Int, offset: Int?, psi: PsiElement?) {
        val task = object : Task.Backgroundable(project, "Generating actions", true) {
            private val workspace = EvaluationWorkspace(workspaceDir, completionType.name)

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                generateActions(workspace, languageName, files, strategy, offset, psi, getProcess(indicator))
            }

            override fun onSuccess() {
                if (interpretActions)
                    interpretUnderProgress(workspace, completionType, strategy, languageName,
                            offset == null, saveLogs, logsTrainingPercentage)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(workspace: EvaluationWorkspace, languageName: String, files: Collection<VirtualFile>,
                                strategy: CompletionStrategy, offset: Int?, psi: PsiElement?, indicator: Progress) {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, languageName, strategy.statement == CompletionStatement.ALL_TOKENS)

        val sortedFiles = files.sortedBy { f -> f.name }
        val errors = mutableListOf<FileErrorInfo>()
        var completed = 0
        for (file in sortedFiles) {
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
            private val sessionsStorage = workspace.sessionsStorage
            private lateinit var lastFileSessions: List<Session>

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val logsPath = Paths.get(workspace.logsDirectory(), languageName.toLowerCase()).toString()
                val logsWatcher = if (saveLogs) DirectoryWatcher(statsCollectorLogsDirectory(), logsPath, logsTrainingPercentage) else null
                lastFileSessions = interpretActions(workspace.actionsStorage, sessionsStorage, completionType, strategy, project, logsWatcher, getProcess(indicator))
            }

            override fun onSuccess() = finish()
            override fun onCancel() = finish()

            private fun finish() {
                if (!generateReport) return Highlighter(project).highlight(lastFileSessions)
                val reportGenerator = HtmlReportGenerator(workspace.reportsDirectory())
                ReportGeneration(reportGenerator).generateReportUnderProgress(listOf(workspace.sessionsStorage), listOf(workspace.errorsStorage), project, isHeadless)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(actionsStorage: ActionsStorage, sessionsStorage: SessionsStorage, completionType: CompletionType, strategy: CompletionStrategy,
                                 project: Project, logsWatcher: DirectoryWatcher?, indicator: Progress): List<Session> {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project, completionType), project)
        var sessionsCount = 0
        val computingTime = measureTimeMillis {
            sessionsCount = actionsStorage.computeSessionsCount()
        }
        LOG.info("Computing of sessions count took $computingTime ms")
        val handler = InterpretationHandlerImpl(indicator, sessionsCount)
        val interpreter = Interpreter(completionInvoker, handler, project.basePath)
        logsWatcher?.start()
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        setMLCompletion(completionType == CompletionType.ML)
        val files = actionsStorage.getActionFiles()
        var lastFileSessions = listOf<Session>()
        for (file in files) {
            try {
                val fileActions = actionsStorage.getActions(file)
                lastFileSessions = interpreter.interpret(fileActions)
                val fileText = FilesHelper.getProjectPath(project, fileActions.path).readText()
                sessionsStorage.saveSessions(FileSessionsInfo(fileActions.path, fileText, lastFileSessions))
            } catch (e: Throwable) {
                LOG.error("Actions interpretation error for file ${file.path}.", e)
            }
            if (handler.isCancelled()) break
        }
        sessionsStorage.saveEvaluationInfo(EvaluationInfo(completionType.name, strategy))
        LOG.info("Interpreting actions completed")
        setMLCompletion(mlCompletionFlag)
        logsWatcher?.stop()
        return lastFileSessions
    }

    private fun getProcess(indicator: ProgressIndicator) = if (isHeadless) CommandLineProgress(indicator.text) else IdeaProgress(indicator)

    private fun statsCollectorLogsDirectory(): String {
        return Paths.get(PathManager.getSystemPath(), "completion-stats-data").toString()
    }

    private fun stackTraceToString(e: Throwable): String {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return sw.toString()
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
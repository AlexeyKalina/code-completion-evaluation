package org.jb.cce

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.stats.storage.PluginDirectoryFilePathProvider
import org.jb.cce.actions.*
import org.jb.cce.highlighter.Highlighter
import org.jb.cce.info.EvaluationInfo
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.info.SessionsEvaluationInfo
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.util.*
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.jb.cce.visitors.EvaluationRootByRangeVisitor
import org.jb.cce.visitors.EvaluationRootByOffsetVisitor
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess

class CompletionEvaluator(private val isHeadless: Boolean) {
    private val minInMs = 60000
    private companion object {
        val LOG = Logger.getInstance(CompletionEvaluator::class.java)
    }

    fun evaluateCompletion(project: Project, files: List<VirtualFile>, languageName: String, strategy: CompletionStrategy,
                           completionTypes: List<CompletionType>, workspaceDir: String, interpretActions: Boolean, saveLogs: Boolean, logsTrainingPercentage: Int) {
        val language2files = FilesHelper.getFiles(files)
        if (language2files.isEmpty()) {
            println("Languages of selected files aren't supported.")
            return finishWork(null)
        }
        evaluateUnderProgress(project, languageName, language2files.getValue(languageName), strategy, completionTypes, workspaceDir, interpretActions, saveLogs, logsTrainingPercentage, null, null)
    }

    fun evaluateCompletionHere(project: Project, file: VirtualFile, languageName: String, offset: Int, psi: PsiElement?,
                               strategy: CompletionStrategy, completionTypes: List<CompletionType>) =
        evaluateUnderProgress(project, languageName, listOf(file), strategy, completionTypes, "", true, false, 0, offset, psi)

    private fun evaluateUnderProgress(project: Project, languageName: String, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                      completionTypes: List<CompletionType>, workspaceDir: String, interpretActions: Boolean, saveLogs: Boolean,
                                      logsTrainingPercentage: Int, offset: Int?, psi: PsiElement?) {
        val task = object : Task.Backgroundable(project, "Generating actions for selected files", true) {
            private lateinit var actions: List<Action>
            private lateinit var errors: List<FileErrorInfo>

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val result = generateActions(project, languageName, files, strategy, offset, psi, getProcess(indicator))
                actions = result.first
                errors = result.second
            }

            override fun onSuccess() {
                val reportGenerator = if (offset == null) HtmlReportGenerator(workspaceDir) else null
                if (actions.isEmpty()) return Messages.showInfoMessage(project, "No tokens for completion", "Nothing to complete")
                if (interpretActions)
                    interpretUnderProgress(actions, errors, completionTypes, strategy, project, languageName, reportGenerator, saveLogs, logsTrainingPercentage)
                else
                    reportGenerator?.saveActions(actions)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(project: Project, languageName: String, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                offset: Int?, psi: PsiElement?, indicator: Progress): Pair<List<Action>, List<FileErrorInfo>> {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, languageName, strategy.statement == CompletionStatement.ALL_TOKENS)

        val sortedFiles = files.sortedBy { f -> f.name }
        val generatedActions = mutableListOf<List<Action>>()
        val errors = mutableListOf<FileErrorInfo>()
        var completed = 0
        for (file in sortedFiles) {
            if (indicator.isCanceled()) {
                LOG.info("Generating actions is canceled by user. Done: $completed/${files.size}. With error: ${errors.size}")
                break
            }
            LOG.info("Start generating actions for file ${file.path}. Done: $completed/${files.size}. With error: ${errors.size}")
            indicator.setProgress(file.name, completed.toDouble() / files.size)
            try {
                val rootVisitor = when {
                    psi != null -> EvaluationRootByRangeVisitor(psi.textRange?.startOffset ?: psi.textOffset,
                            psi.textRange?.endOffset ?:psi.textOffset + psi.textLength)
                    offset != null -> EvaluationRootByOffsetVisitor(offset, file.path, file.text())
                    else -> DefaultEvaluationRootVisitor()
                }
                val uast = uastBuilder.build(file, rootVisitor)
                generatedActions.add(actionsGenerator.generate(uast))
            } catch (e: Exception) {
                errors.add(FileErrorInfo(file.path, e))
                LOG.error("Error for file ${file.path}. Message: ${e.message}")
            }
            completed++
            LOG.info("Generating actions for file ${file.path} completed. Done: $completed/${files.size}. With error: ${errors.size}")
        }

        return Pair(generatedActions.flatten(), errors)
    }

    private fun interpretUnderProgress(actions: List<Action>, errors: List<FileErrorInfo>, completionTypes: List<CompletionType>, strategy: CompletionStrategy,
                                       project: Project, languageName: String, reportGenerator: HtmlReportGenerator?, saveLogs: Boolean, logsTrainingPercentage: Int) {
        val task = object : Task.Backgroundable(project, "Interpretation of the generated actions") {
            private var sessionsInfo: List<SessionsEvaluationInfo>? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val logsPath = if (reportGenerator == null) "" else Paths.get(reportGenerator.logsDirectory(), languageName.toLowerCase()).toString()
                reportGenerator?.saveActions(actions)
                sessionsInfo = interpretActions(actions, completionTypes, strategy, project, logsPath, saveLogs, logsTrainingPercentage, getProcess(indicator))
            }

            override fun onSuccess() = finish()
            override fun onCancel() = finish()

            private fun finish() {
                val sessions = sessionsInfo ?: return finishWork(null)
                if (reportGenerator == null) return Highlighter(project).highlight(sessions)
                val reportPath = generateReport(reportGenerator, sessions, errors)
                finishWork(reportPath)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(actions: List<Action>, completionTypes: List<CompletionType>, strategy: CompletionStrategy,
                                 project: Project, workspaceDir: String, saveLogs: Boolean, logsTrainingPercentage: Int, indicator: Progress): List<SessionsEvaluationInfo> {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project))
        val interpreter = Interpreter(completionInvoker)
        val logsWatcher = if (saveLogs) DirectoryWatcher(PluginDirectoryFilePathProvider().getStatsDataDirectory().toString(), workspaceDir, logsTrainingPercentage) else null
        logsWatcher?.start()

        val sessionsInfo = mutableListOf<SessionsEvaluationInfo>()
        val actionStats = mutableListOf<ActionStat>()
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        var completed = 0
        for (completionType in completionTypes) {
            setMLCompletion(completionType == CompletionType.ML)
            val fileSessions = mutableListOf<FileEvaluationInfo<Session>>()
            interpreter.interpret(actions, completionType) { sessions, stats, filePath, fileText, actionsDone ->
                completed += actionsDone
                fileSessions.add(FileEvaluationInfo(filePath, sessions, fileText))
                actionStats.addAll(stats)
                val perMinute = actionStats.count { it.timestamp > Date().time - minInMs }
                indicator.setProgress("$completionType ${File(filePath).name} ($completed/${actions.size * completionTypes.size} act, $perMinute act/min)",
                        completed.toDouble() / (actions.size * completionTypes.size))
                LOG.info("Interpreting actions for file $filePath ($completionType completion) completed. Done: $completed/${actions.size * completionTypes.size}, $perMinute act/min")
                if (indicator.isCanceled()) {
                    LOG.info("Interpreting actions is canceled by user.")
                    logsWatcher?.stop()
                    return@interpret true
                }
                return@interpret false
            }
            sessionsInfo.add(SessionsEvaluationInfo(fileSessions, EvaluationInfo(completionType.name, strategy)))
        }
        setMLCompletion(mlCompletionFlag)
        logsWatcher?.stop()
        return sessionsInfo
    }

    private fun getProcess(indicator: ProgressIndicator) = if (isHeadless) CommandLineProgress(indicator.text) else IdeaProgress(indicator)

    private fun finishWork(reportPath: String?) {
        if (reportPath == null)
            if (isHeadless) exitProcess(1) else return

        if (isHeadless) {
            println("Evaluation completed. Report: $reportPath")
            exitProcess(0)
        } else {
            ApplicationManager.getApplication().invokeAndWait {
                if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
            }
        }
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
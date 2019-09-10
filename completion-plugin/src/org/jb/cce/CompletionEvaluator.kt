package org.jb.cce

import com.intellij.openapi.application.PathManager
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
import com.intellij.util.io.isFile
import com.intellij.util.io.readText
import org.jb.cce.actions.*
import org.jb.cce.highlighter.Highlighter
import org.jb.cce.info.EvaluationInfo
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.SessionsEvaluationInfo
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.interpretator.InterpretationHandlerImpl
import org.jb.cce.util.*
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.jb.cce.visitors.EvaluationRootByOffsetVisitor
import org.jb.cce.visitors.EvaluationRootByRangeVisitor
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class CompletionEvaluator(private val isHeadless: Boolean) {
    private companion object {
        val LOG = Logger.getInstance(CompletionEvaluator::class.java)
        private val actionsSerializer = ActionSerializer()
    }
    private var actionsSize = 0

    fun evaluateCompletion(project: Project, files: List<VirtualFile>, languageName: String, strategy: CompletionStrategy,
                           completionType: CompletionType, workspaceDir: String, interpretActions: Boolean, saveLogs: Boolean, logsTrainingPercentage: Int) {
        val language2files = FilesHelper.getFiles(project, files)
        if (language2files.isEmpty()) {
            println("Languages of selected files aren't supported.")
            return finishWork(null, isHeadless)
        }
        evaluateUnderProgress(project, languageName, language2files.getValue(languageName), strategy, completionType, workspaceDir, interpretActions, saveLogs, logsTrainingPercentage, null, null)
    }

    fun evaluateCompletionHere(project: Project, file: VirtualFile, languageName: String, offset: Int, psi: PsiElement?,
                               strategy: CompletionStrategy, completionType: CompletionType) =
            evaluateUnderProgress(project, languageName, listOf(file), strategy, completionType, "", true, false, 0, offset, psi)

    private fun evaluateUnderProgress(project: Project, languageName: String, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                      completionType: CompletionType, workspaceDir: String, interpretActions: Boolean, saveLogs: Boolean,
                                      logsTrainingPercentage: Int, offset: Int?, psi: PsiElement?) {
        val task = object : Task.Backgroundable(project, "Generating actions for selected files", true) {
            private lateinit var errors: List<FileErrorInfo>
            private val reportGenerator = HtmlReportGenerator(workspaceDir)

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                errors = generateActions(project, languageName, files, strategy, offset, psi, reportGenerator, getProcess(indicator))
            }

            override fun onSuccess() {
                if (actionsSize == 0) return Messages.showInfoMessage(project, "No tokens for completion", "Nothing to complete")
                if (interpretActions)
                    interpretUnderProgress(errors, completionType, strategy, project, languageName, reportGenerator,
                            offset == null, saveLogs, logsTrainingPercentage)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(project: Project, languageName: String, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                offset: Int?, psi: PsiElement?, reportGenerator: HtmlReportGenerator, indicator: Progress): List<FileErrorInfo> {
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
            indicator.setProgress(file.name, completed.toDouble() / files.size)
            try {
                val rootVisitor = when {
                    psi != null -> EvaluationRootByRangeVisitor(psi.textRange?.startOffset ?: psi.textOffset,
                            psi.textRange?.endOffset ?:psi.textOffset + psi.textLength)
                    offset != null -> EvaluationRootByOffsetVisitor(offset, file.path, file.text())
                    else -> DefaultEvaluationRootVisitor()
                }
                val uast = uastBuilder.build(file, rootVisitor)
                val actions = actionsGenerator.generate(uast)
                reportGenerator.saveActions(actions, file.name)
                actionsSize += actions.size
            } catch (e: Exception) {
                errors.add(FileErrorInfo(file.path, e))
                LOG.error("Error for file ${file.path}.", e)
            }
            completed++
            LOG.info("Generating actions for file ${file.path} completed. Done: $completed/${files.size}. With error: ${errors.size}")
        }

        return errors
    }

    private fun interpretUnderProgress(errors: List<FileErrorInfo>, completionType: CompletionType, strategy: CompletionStrategy, project: Project,
                                       languageName: String, reportGenerator: HtmlReportGenerator, generateReport: Boolean, saveLogs: Boolean, logsTrainingPercentage: Int) {
        val task = object : Task.Backgroundable(project, "Interpretation of the generated actions") {
            private var sessionsInfo: List<SessionsEvaluationInfo>? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val logsPath = Paths.get(reportGenerator.logsDirectory(), languageName.toLowerCase()).toString()
                val actionsDir = reportGenerator.actionsDirectory()
                sessionsInfo = interpretActions(actionsDir, completionType, strategy, project, logsPath, saveLogs, logsTrainingPercentage, getProcess(indicator))
            }

            override fun onSuccess() = finish()
            override fun onCancel() = finish()

            private fun finish() {
                val sessions = sessionsInfo ?: return finishWork(null, isHeadless)
                if (!generateReport) return Highlighter(project).highlight(sessions)
                generateReportUnderProgress(sessions, errors, reportGenerator, project, isHeadless)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(pathToActions: String, completionType: CompletionType, strategy: CompletionStrategy, project: Project,
                                 workspaceDir: String, saveLogs: Boolean, logsTrainingPercentage: Int, indicator: Progress): List<SessionsEvaluationInfo> {
        val interpreter = Interpreter()
        val logsWatcher = if (saveLogs) DirectoryWatcher(statsCollectorLogsDirectory(), workspaceDir, logsTrainingPercentage) else null
        logsWatcher?.start()
        val sessionsInfo = mutableListOf<SessionsEvaluationInfo>()
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project, completionType))
        setMLCompletion(completionType == CompletionType.ML)
        val handler = InterpretationHandlerImpl(indicator, actionsSize)
        Files.walk(Paths.get(pathToActions)).use { paths ->
            paths.filter { it.isFile() }.forEach {
                val actions = actionsSerializer.deserialize(it.readText())
                interpreter.interpret(completionInvoker, actions, handler)
            }
        }
        sessionsInfo.add(SessionsEvaluationInfo(handler.getSessions(), EvaluationInfo(completionType.name, strategy)))
        setMLCompletion(mlCompletionFlag)
        logsWatcher?.stop()
        return sessionsInfo
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
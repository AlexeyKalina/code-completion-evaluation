package org.jb.cce.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import org.jb.cce.*
import org.jb.cce.info.*
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.metrics.MetricInfo
import org.jb.cce.metrics.MetricsEvaluator
import org.jb.cce.uast.Language
import java.io.File

class EvaluateCompletionForSelectedFilesAction : AnAction() {
    private companion object {
        val LOG = Logger.getInstance(EvaluateCompletionForSelectedFilesAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val language2files = getFiles(e)
        if (language2files.isEmpty()) {
            Messages.showInfoMessage(project, "Languages of selected files aren't supported.", "Nothing to complete")
            return
        }

        val settingsDialog = CompletionSettingsDialog(project, language2files)
        val result = settingsDialog.showAndGet()
        if (!result) return

        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionContext)
        val completionTypes = settingsDialog.completionTypes
        val task = object : Task.Backgroundable(project, "Generating actions for selected files", true) {
            private lateinit var actions: List<Action>
            private lateinit var errors: List<FileErrorInfo>
            private val files = language2files.getValue(settingsDialog.language)

            override fun run(indicator: ProgressIndicator) {
                val results = generateActions(project, settingsDialog.language, files, strategy, indicator)
                actions = results.first
                errors = results.second
            }

            override fun onSuccess() {
                interpretUnderProgress(actions, errors, files.size - errors.size, completionTypes, strategy, project, settingsDialog.outputDir)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(project: Project, language: Language, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                indicator: ProgressIndicator): Pair<List<Action>, List<FileErrorInfo>> {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, language)

        val sortedFiles = files.sortedBy { f -> f.name }
        val generatedActions = mutableListOf<List<Action>>()
        val errors = mutableListOf<FileErrorInfo>()
        var completed = 0
        for (file in sortedFiles) {
            if (indicator.isCanceled) {
                LOG.info("Generating actions is canceled by user. Done: $completed/${files.size}. With error: ${errors.size}")
                break
            }
            LOG.info("Start generating actions for file ${file.path}. Done: $completed/${files.size}. With error: ${errors.size}")
            indicator.text2 = file.name
            indicator.fraction = completed.toDouble() / files.size
            try {
                val uast = uastBuilder.build(file)
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

    private fun interpretUnderProgress(actions: List<Action>, errors: List<FileErrorInfo>, filesCount: Int, completionTypes: List<CompletionType>,
                                       strategy: CompletionStrategy, project: Project, outputDir: String) {
        val task = object : Task.Backgroundable(project, "Interpretation of the generated actions") {
            private var sessionsInfo: List<SessionsEvaluationInfo>? = null

            override fun run(indicator: ProgressIndicator) {
                sessionsInfo = interpretActions(actions, filesCount, completionTypes, strategy, project, indicator)
            }

            override fun onSuccess() {
                if (sessionsInfo == null) return
                val metricsInfo = evaluateMetrics(sessionsInfo!!)
                generateReports(outputDir, sessionsInfo!!, metricsInfo, errors)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(actions: List<Action>, filesCount: Int, completionTypes: List<CompletionType>,
                                 strategy: CompletionStrategy, project: Project, indicator: ProgressIndicator): List<SessionsEvaluationInfo> {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project))
        val interpreter = Interpreter(completionInvoker)

        val sessionsInfo = mutableListOf<SessionsEvaluationInfo>()
        var completed = 0
        LOG.info("Start interpreting actions")
        for (completionType in completionTypes) {
            if (completionType == CompletionType.ML) setMLCompletion(true)
            val fileSessions = mutableListOf<FileEvaluationInfo<Session>>()
            interpreter.interpret(actions, completionType) { sessions, filePath, fileText, isCanceled ->
                if (indicator.isCanceled) {
                    LOG.info("Interpreting actions is canceled by user.")
                    isCanceled.value = true
                    return@interpret
                }
                fileSessions.add(FileEvaluationInfo(filePath, sessions, fileText))
                completed++
                indicator.text2 = "$completionType ${File(filePath).name}"
                indicator.fraction = completed.toDouble() / (filesCount * completionTypes.size)
                LOG.info("Interpreting actions for file $filePath ($completionType completion) completed. Done: $completed/${filesCount * completionTypes.size}")
            }
            if (completionType == CompletionType.ML) setMLCompletion(false)
            sessionsInfo.add(SessionsEvaluationInfo(fileSessions, EvaluationInfo(completionType.name, strategy)))
        }
        if (!indicator.isCanceled) return sessionsInfo
        return emptyList()
    }

    private fun evaluateMetrics(evaluationsInfo: List<SessionsEvaluationInfo>): List<MetricsEvaluationInfo> {
        val metricsInfo = mutableListOf<MetricsEvaluationInfo>()
        for (sessionsInfo in evaluationsInfo) {
            val metricsEvaluator = MetricsEvaluator.withDefaultMetrics()
            val filesInfo = mutableListOf<FileEvaluationInfo<MetricInfo>>()
            for (file in sessionsInfo.sessions) {
                filesInfo.add(FileEvaluationInfo(file.filePath, metricsEvaluator.evaluate(file.results), file.text))
            }
            metricsInfo.add(MetricsEvaluationInfo(metricsEvaluator.result(), filesInfo, sessionsInfo.info))
        }
        return metricsInfo
    }

    private fun generateReports(outputDir: String, sessions: List<SessionsEvaluationInfo>, metrics: List<MetricsEvaluationInfo>,
                                errors: List<FileErrorInfo>) {
        val reportGenerator = HtmlReportGenerator(outputDir)
        val reportPath = reportGenerator.generateReport(sessions, metrics, errors)
        ApplicationManager.getApplication().invokeAndWait {
            if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
        }
    }

    private fun getFiles(e: AnActionEvent): Map<Language, Set<VirtualFile>> {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyMap()
        val language2files = mutableMapOf<Language, MutableSet<VirtualFile>>()
        for (file in selectedFiles) {
            VfsUtilCore.iterateChildrenRecursively(file, VirtualFileFilter.ALL, object : ContentIterator {
                override fun processFile(fileOrDir: VirtualFile): Boolean {
                    val extension = fileOrDir.extension
                    if (fileOrDir.isDirectory || extension == null) return true

                    val language = Language.resolve(extension)
                    if (language != Language.UNSUPPORTED) {
                        language2files.computeIfAbsent(language) { mutableSetOf() }.add(fileOrDir)
                    }
                    return true
                }
            })
        }
        return language2files
    }

    private fun setMLCompletion(start: Boolean) {
        Registry.get(PropertKey@ "completion.stats.enable.ml.ranking").setValue(start)
    }
}
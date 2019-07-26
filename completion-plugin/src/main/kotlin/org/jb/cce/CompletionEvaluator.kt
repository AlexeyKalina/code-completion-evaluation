package org.jb.cce

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import org.jb.cce.actions.*
import org.jb.cce.info.*
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.metrics.MetricInfo
import org.jb.cce.metrics.MetricsEvaluator
import org.jb.cce.uast.Language
import java.io.File

class CompletionEvaluator(private val isHeadless: Boolean) {
    private companion object {
        val LOG = Logger.getInstance(CompletionEvaluator::class.java)
    }

    fun evaluateCompletion(project: Project, files: List<VirtualFile>, language: Language, strategy: CompletionStrategy,
                           completionTypes: List<CompletionType>, outputDir: String) {
        val language2files = getFiles(files)
        if (language2files.isEmpty()) {
            print("Languages of selected files aren't supported.")
            return
        }
        evaluateUnderProgress(project, language, language2files.getValue(language), strategy, completionTypes, outputDir)
    }

    fun getFiles(selectedFiles: List<VirtualFile>): Map<Language, Set<VirtualFile>> {
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

    private fun evaluateUnderProgress(project: Project, language: Language, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                      completionTypes: List<CompletionType>, outputDir: String) {
        val task = object : Task.Backgroundable(project, "Generating actions for selected files", true) {
            private lateinit var actions: List<Action>
            private lateinit var errors: List<FileErrorInfo>

            override fun run(indicator: ProgressIndicator) {
                val result = generateActions(project, language, files, strategy, indicator)
                actions = result.first
                errors = result.second
            }

            override fun onSuccess() {
                interpretUnderProgress(actions, errors, completionTypes, strategy, project, outputDir)
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

    private fun interpretUnderProgress(actions: List<Action>, errors: List<FileErrorInfo>, completionTypes: List<CompletionType>,
                                       strategy: CompletionStrategy, project: Project, outputDir: String) {
        val task = object : Task.Backgroundable(project, "Interpretation of the generated actions") {
            private var sessionsInfo: List<SessionsEvaluationInfo>? = null

            override fun run(indicator: ProgressIndicator) {
                sessionsInfo = interpretActions(actions, completionTypes, strategy, project, indicator)
            }

            override fun onSuccess() {
                val sessions = sessionsInfo ?: return
                val metricsInfo = evaluateMetrics(sessions)
                generateReports(outputDir, sessions, metricsInfo, errors)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(actions: List<Action>, completionTypes: List<CompletionType>,
                                 strategy: CompletionStrategy, project: Project, indicator: ProgressIndicator): List<SessionsEvaluationInfo> {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project))
        val interpreter = Interpreter(completionInvoker)

        val sessionsInfo = mutableListOf<SessionsEvaluationInfo>()
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        var completed = 0
        for (completionType in completionTypes) {
            setMLCompletion(completionType == CompletionType.ML)
            val fileSessions = mutableListOf<FileEvaluationInfo<Session>>()
            interpreter.interpret(actions, completionType) { sessions, filePath, fileText, actionsDone ->
                if (indicator.isCanceled) {
                    LOG.info("Interpreting actions is canceled by user.")
                    return@interpret true
                }
                completed += actionsDone
                fileSessions.add(FileEvaluationInfo(filePath, sessions, fileText))
                indicator.text2 = "$completionType ${File(filePath).name}"
                indicator.fraction = completed.toDouble() / (actions.size * completionTypes.size)
                LOG.info("Interpreting actions for file $filePath ($completionType completion) completed. Done: $completed/${actions.size * completionTypes.size}")
                return@interpret false
            }
            sessionsInfo.add(SessionsEvaluationInfo(fileSessions, EvaluationInfo(completionType.name, strategy)))
        }
        setMLCompletion(mlCompletionFlag)
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
            if (!isHeadless && OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
        }
    }

    private fun isMLCompletionEnabled() = Registry.get(PropertKey@ "completion.stats.enable.ml.ranking").asBoolean()
    private fun setMLCompletion(value: Boolean) = Registry.get(PropertKey@ "completion.stats.enable.ml.ranking").setValue(value)
}
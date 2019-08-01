package org.jb.cce

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.stats.storage.PluginDirectoryFilePathProvider
import org.jb.cce.actions.*
import org.jb.cce.info.*
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.metrics.MetricInfo
import org.jb.cce.metrics.MetricsEvaluator
import org.jb.cce.uast.Language
import org.jb.cce.util.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess

class CompletionEvaluator(private val isHeadless: Boolean) {
    private companion object {
        val LOG = Logger.getInstance(CompletionEvaluator::class.java)
    }

    fun evaluateCompletion(project: Project, files: List<VirtualFile>, language: Language, strategy: CompletionStrategy,
                           completionTypes: List<CompletionType>, outputDir: String, saveLogs: Boolean, actionsPath: String?, externalSessionsPath: String?) {
        val language2files = FilesHelper.getFiles(files)
        if (language2files.isEmpty()) {
            println("Languages of selected files aren't supported.")
            return finishWork(null)
        }
        evaluateUnderProgress(project, language, language2files.getValue(language), strategy, completionTypes, outputDir, saveLogs, actionsPath, externalSessionsPath)
    }

    private fun evaluateUnderProgress(project: Project, language: Language, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                      completionTypes: List<CompletionType>, outputDir: String, saveLogs: Boolean, actionsPath: String?, externalSessionsPath: String?) {
        val task = object : Task.Backgroundable(project, "Generating actions for selected files", true) {
            private lateinit var actions: List<Action>
            private lateinit var errors: List<FileErrorInfo>

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val result = generateActions(project, language, files, strategy, getProcess(indicator))
                actions = result.first
                errors = result.second
            }

            override fun onSuccess() {
                if (actionsPath == null)
                    interpretUnderProgress(actions, errors, completionTypes, strategy, project, language, outputDir, saveLogs, externalSessionsPath)
                else
                    FileWriter(actionsPath).use { it.write(ActionSerializer().serialize(actions)) }
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(project: Project, language: Language, files: Collection<VirtualFile>, strategy: CompletionStrategy,
                                indicator: Progress): Pair<List<Action>, List<FileErrorInfo>> {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, language)

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

    private fun interpretUnderProgress(actions: List<Action>, errors: List<FileErrorInfo>, completionTypes: List<CompletionType>, strategy: CompletionStrategy,
                                       project: Project, language: Language, outputDir: String, saveLogs: Boolean, externalSessionsPath: String?) {
        val task = object : Task.Backgroundable(project, "Interpretation of the generated actions") {
            private var sessionsInfo: List<SessionsEvaluationInfo>? = null
            private val reportGenerator = HtmlReportGenerator(outputDir)

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val logsPath = Paths.get(reportGenerator.logsDirectory(), language.displayName.toLowerCase()).toString()
                sessionsInfo = interpretActions(actions, completionTypes, strategy, project, logsPath, saveLogs, externalSessionsPath, getProcess(indicator))
            }

            override fun onSuccess() {
                val sessions = sessionsInfo ?: return
                val metricsInfo = evaluateMetrics(sessions)
                val reportPath = reportGenerator.generateReport(sessions, metricsInfo, errors)
                finishWork(reportPath)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(actions: List<Action>, completionTypes: List<CompletionType>, strategy: CompletionStrategy,
                                 project: Project, outputDir: String, saveLogs: Boolean, externalSessionsPath: String?, indicator: Progress): List<SessionsEvaluationInfo> {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project))
        val interpreter = Interpreter(completionInvoker)
        val logsWatcher = if (saveLogs) DirectoryWatcher(PluginDirectoryFilePathProvider().getStatsDataDirectory().toString(), outputDir) else null
        logsWatcher?.start()

        val sessionsInfo = mutableListOf<SessionsEvaluationInfo>()
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        var completed = 0
        for (completionType in completionTypes) {
            setMLCompletion(completionType == CompletionType.ML)
            val fileSessions = mutableListOf<FileEvaluationInfo<Session>>()
            interpreter.interpret(actions, completionType) { sessions, filePath, fileText, actionsDone ->
                if (indicator.isCanceled()) {
                    LOG.info("Interpreting actions is canceled by user.")
                    logsWatcher?.stop()
                    return@interpret true
                }
                completed += actionsDone
                fileSessions.add(FileEvaluationInfo(filePath, sessions, fileText))
                indicator.setProgress("$completionType ${File(filePath).name}", completed.toDouble() / (actions.size * completionTypes.size))
                LOG.info("Interpreting actions for file $filePath ($completionType completion) completed. Done: $completed/${actions.size * completionTypes.size}")
                return@interpret false
            }
            sessionsInfo.add(SessionsEvaluationInfo(fileSessions, EvaluationInfo(completionType.name, strategy)))
        }
        if (externalSessionsPath != null) {
            val info = SessionSerializer().deserialize(FileReader(externalSessionsPath).use { it.readText() })
            sessionsInfo.add(info)
        }
        setMLCompletion(mlCompletionFlag)
        logsWatcher?.stop()
        if (!indicator.isCanceled()) return sessionsInfo
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
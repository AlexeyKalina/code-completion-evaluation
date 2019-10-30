package org.jb.cce.evaluation

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.Interpreter
import org.jb.cce.Session
import org.jb.cce.actions.CompletionType
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import org.jb.cce.highlighter.Highlighter
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileSessionsInfo
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.interpretator.InterpretationHandlerImpl
import org.jb.cce.storages.ActionsStorage
import org.jb.cce.storages.FileErrorsStorage
import org.jb.cce.storages.SessionsStorage
import org.jb.cce.util.*
import java.nio.file.Paths
import java.util.*
import kotlin.system.measureTimeMillis

class ActionsInterpretationEvaluator(project: Project, isHeadless: Boolean): BaseEvaluator(project, isHeadless) {

    fun evaluateUnderProgress(actionsWorkspace: EvaluationWorkspace, config: Config, createWorkspace: Boolean, generateReport: Boolean, highlight: Boolean) {
        val task = object : Task.Backgroundable(project, "Actions interpreting") {
            private lateinit var lastFileSessions: List<Session>
            private var sessionsWorkspace =
                    if (createWorkspace) EvaluationWorkspace(config.outputDir)
                    else actionsWorkspace

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                if (createWorkspace) ConfigFactory.save(config, sessionsWorkspace.path())
                if (config.saveLogs) sessionsWorkspace.logsStorage.watch(statsCollectorLogsDirectory(), config.language, config.trainTestSplit)
                lastFileSessions = interpretActions(actionsWorkspace.actionsStorage, sessionsWorkspace.sessionsStorage,
                        sessionsWorkspace.errorsStorage, config.completionType, project, getProcess(indicator))
            }

            override fun onSuccess() {
                if (config.saveLogs) sessionsWorkspace.logsStorage.stopWatching()
                if (highlight) Highlighter(project).highlight(lastFileSessions)
                if (generateReport) {
                    val reportGenerator = HtmlReportGenerator(sessionsWorkspace.reportsDirectory())
                    val evaluator = ReportGenerationEvaluator(reportGenerator, project, isHeadless)
                    evaluator.generateReportUnderProgress(listOf(sessionsWorkspace.sessionsStorage), listOf(sessionsWorkspace.errorsStorage))
                } else finisher.onSuccess()
            }

            override fun onCancel() = finisher.onCancel(this.title)

            override fun onThrowable(error: Throwable) = finisher.onError(error, this.title)
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(actionsStorage: ActionsStorage, sessionsStorage: SessionsStorage, errorsStorage: FileErrorsStorage, completionType: CompletionType, project: Project, indicator: Progress): List<Session> {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project, completionType), project)
        var sessionsCount = 0
        val computingTime = measureTimeMillis {
            sessionsCount = actionsStorage.computeSessionsCount()
        }
        LOG.info("Computing of sessions count took $computingTime ms")
        val handler = InterpretationHandlerImpl(indicator, sessionsCount)
        val interpreter = Interpreter(completionInvoker, handler, project.basePath)
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        setMLCompletion(completionType == CompletionType.ML)
        val files = actionsStorage.getActionFiles()
        var lastFileSessions = listOf<Session>()
        for (file in files) {
            val fileActions = actionsStorage.getActions(file)
            try {
                lastFileSessions = interpreter.interpret(fileActions)
                val fileText = FilesHelper.getFile(project, fileActions.path).text()
                sessionsStorage.saveSessions(FileSessionsInfo(fileActions.path, fileText, lastFileSessions))
            } catch (e: Throwable) {
                errorsStorage.saveError(FileErrorInfo(fileActions.path, e.message ?: "No Message", stackTraceToString(e)))
                LOG.error("Actions interpretation error for file $file.", e)
            }
            if (handler.isCancelled()) break
        }
        sessionsStorage.saveEvaluationInfo()
        LOG.info("Interpreting actions completed")
        setMLCompletion(mlCompletionFlag)
        return lastFileSessions
    }

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
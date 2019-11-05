package org.jb.cce.evaluation.step

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.Interpreter
import org.jb.cce.Session
import org.jb.cce.actions.CompletionType
import org.jb.cce.evaluation.ActionsInterpretationHandler
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
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

class ActionsInterpretationStep(
        private val config: Config.ActionsInterpretation,
        private val language: String,
        project: Project,
        isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Actions interpreting"

    override val description: String = "Interpretation of generated actions"

    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val result = FutureResult<EvaluationWorkspace?>()
        val task = object : Task.Backgroundable(project, name) {

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                ActionsInterpretationHandler(config, language, project).invoke(workspace, workspace, getProgress(indicator))
            }

            override fun onSuccess() {
                result.set(workspace)
            }

            override fun onCancel() {
                evaluationAbortedHandler.onCancel(this.title)
                result.set(null)
            }

            override fun onThrowable(error: Throwable) {
                evaluationAbortedHandler.onError(error, this.title)
                result.set(null)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        return result.get()
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
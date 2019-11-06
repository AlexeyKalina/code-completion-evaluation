package org.jb.cce.evaluation

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.Interpreter
import org.jb.cce.actions.CompletionType
import org.jb.cce.exceptions.ExceptionsUtil
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileSessionsInfo
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.interpretator.InterpretationHandlerImpl
import org.jb.cce.util.FilesHelper
import org.jb.cce.util.Progress
import org.jb.cce.util.text
import java.nio.file.Paths
import java.util.*
import kotlin.system.measureTimeMillis

class ActionsInterpretationHandler(private val project: Project) : TwoWorkspaceHandler {
    companion object {
        val LOG = Logger.getInstance(ActionsInterpretationHandler::class.java)
    }

    override fun invoke(workspace1: EvaluationWorkspace, workspace2: EvaluationWorkspace, indicator: Progress) {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project, workspace2.config.interpret.completionType), project)
        var sessionsCount = 0
        val computingTime = measureTimeMillis {
            sessionsCount = workspace1.actionsStorage.computeSessionsCount()
        }
        LOG.info("Computing of sessions count took $computingTime ms")
        val handler = InterpretationHandlerImpl(indicator, sessionsCount)
        val interpreter = Interpreter(completionInvoker, handler, project.basePath)
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        setMLCompletion(workspace2.config.interpret.completionType == CompletionType.ML)
        if (workspace2.config.interpret.saveLogs)
            workspace2.logsStorage.watch(statsCollectorLogsDirectory(), workspace2.config.language, workspace2.config.interpret.trainTestSplit)
        val files = workspace1.actionsStorage.getActionFiles()
        for (file in files) {
            val fileActions = workspace1.actionsStorage.getActions(file)
            try {
                val sessions = interpreter.interpret(fileActions)
                val fileText = FilesHelper.getFile(project, fileActions.path).text()
                workspace2.sessionsStorage.saveSessions(FileSessionsInfo(fileActions.path, fileText, sessions))
            } catch (e: Throwable) {
                workspace2.errorsStorage.saveError(FileErrorInfo(fileActions.path, e.message ?: "No Message", ExceptionsUtil.stackTraceToString(e)))
                LOG.error("Actions interpretation error for file $file.", e)
            }
            if (handler.isCancelled()) break
        }
        if (workspace2.config.interpret.saveLogs) workspace2.logsStorage.stopWatching()
        workspace2.sessionsStorage.saveEvaluationInfo()
        LOG.info("Interpreting actions completed")
        setMLCompletion(mlCompletionFlag)
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
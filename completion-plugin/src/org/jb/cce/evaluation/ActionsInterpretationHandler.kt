package org.jb.cce.evaluation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.serviceContainer.PlatformComponentManagerImpl
import com.intellij.stats.sender.StatisticSender
import com.intellij.stats.storage.FilePathProvider
import com.intellij.stats.storage.UniqueFilesProvider
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.InterpretFilter
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
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.system.measureTimeMillis

class ActionsInterpretationHandler(
        private val config: Config.ActionsInterpretation,
        private val language: String,
        private val project: Project) : TwoWorkspaceHandler {
    companion object {
        val LOG = Logger.getInstance(ActionsInterpretationHandler::class.java)
    }

    override fun invoke(workspace1: EvaluationWorkspace, workspace2: EvaluationWorkspace, indicator: Progress) {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project, config.completionType), project)
        var sessionsCount = 0
        val computingTime = measureTimeMillis {
            sessionsCount = workspace1.actionsStorage.computeSessionsCount()
        }
        LOG.info("Computing of sessions count took $computingTime ms")
        val handler = InterpretationHandlerImpl(indicator, sessionsCount)
        val filter =
                if (config.completeTokenProbability < 1) RandomInterpretFilter(config.completeTokenProbability, config.completeTokenSeed)
                else InterpretFilter.default()
        val interpreter = Interpreter(completionInvoker, handler, filter, project.basePath)
        val mlCompletionFlag = isMLCompletionEnabled()
        LOG.info("Start interpreting actions")
        setMLCompletion(config.completionType == CompletionType.ML)
        if (config.saveLogs) replaceStatCollectorServices()
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
        if (config.saveLogs) workspace2.logsStorage.save(statsCollectorLogsDirectory(), language, config.trainTestSplit)
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

    private fun replaceStatCollectorServices() {
        File(statsCollectorLogsDirectory()).deleteRecursively()
        val serviceManager = ApplicationManager.getApplication() as PlatformComponentManagerImpl
        val filesProvider = object : UniqueFilesProvider("chunk", PathManager.getSystemPath(), "completion-stats-data") {
            override fun cleanupOldFiles() = Unit
        }
        val statisticSender = object : StatisticSender {
            override fun sendStatsData(url: String) = Unit
        }
        serviceManager.replaceServiceInstance(FilePathProvider::class.java, filesProvider, project)
        serviceManager.replaceServiceInstance(StatisticSender::class.java, statisticSender, project)
    }
}
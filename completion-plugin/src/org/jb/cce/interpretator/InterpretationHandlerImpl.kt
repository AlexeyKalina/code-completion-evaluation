package org.jb.cce.interpretator

import com.intellij.openapi.diagnostic.Logger
import org.jb.cce.InterpretationHandler
import org.jb.cce.Session
import org.jb.cce.actions.ActionStat
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.util.Progress
import java.io.File
import java.util.*

class InterpretationHandlerImpl(private val indicator: Progress, private val actionsCount: Int) : InterpretationHandler {
    private companion object {
        val LOG = Logger.getInstance(InterpretationHandlerImpl::class.java)
    }

    private val minInMs = 60000
    private var completed = 0
    private var currentFilePath: String? = null
    private var currentFileText: String? = null
    private val fileSessions = mutableListOf<FileEvaluationInfo<Session>>()
    private val actionStats = mutableListOf<ActionStat>()

    fun getSessions(): List<FileEvaluationInfo<Session>> = fileSessions

    override fun invokeOnCompletion(stats: List<ActionStat>): Boolean {
        completed += stats.size
        return updateProgress(stats)
    }

    override fun invokeOnFile(sessions: List<Session>, stats: List<ActionStat>, path: String, text: String): Boolean {
        completed += stats.size
        val currentPath = currentFilePath
        val currentText = currentFileText
        if (currentPath != null && currentText != null) {
            fileSessions.add(FileEvaluationInfo(currentPath, sessions, currentText))
            LOG.info("Interpreting actions for file $currentPath completed. Done: $completed/$actionsCount")
        }
        LOG.info("Interpreting actions for file $path started. Done: $completed/$actionsCount")
        currentFilePath = path
        currentFileText = text
        return updateProgress(stats)
    }

    override fun invokeOnError(error: Throwable) {
        LOG.error(error)
    }

    private fun updateProgress(stats: List<ActionStat>): Boolean {
        actionStats.addAll(stats)
        val perMinute = actionStats.count { it.timestamp > Date().time - minInMs }
        indicator.setProgress("${File(currentFilePath!!).name} ($completed/$actionsCount act, $perMinute act/min)",
                completed.toDouble() / actionsCount)
        if (indicator.isCanceled()) {
            LOG.info("Interpreting actions is canceled by user.")
            return true
        }
        return false
    }
}
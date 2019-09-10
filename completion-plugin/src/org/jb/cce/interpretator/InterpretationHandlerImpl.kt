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
    private val fileSessions = mutableListOf<FileEvaluationInfo<Session>>()
    private val actionStats = mutableListOf<ActionStat>()

    fun getSessions(): List<FileEvaluationInfo<Session>> = fileSessions

    override fun invokeOnCompletion(stats: List<ActionStat>, path: String): Boolean {
        completed += stats.size
        return updateProgress(stats, path)
    }

    override fun invokeOnFile(sessions: List<Session>, stats: List<ActionStat>, path: String, text: String): Boolean {
        completed += stats.size
            fileSessions.add(FileEvaluationInfo(path, sessions, text))
            LOG.info("Interpreting actions for file $path completed. Done: $completed/$actionsCount")
        LOG.info("Interpreting actions for file $path started. Done: $completed/$actionsCount")
        return updateProgress(stats, path)
    }

    override fun invokeOnError(error: Throwable) {
        LOG.error(error)
    }

    private fun updateProgress(stats: List<ActionStat>, path: String): Boolean {
        actionStats.addAll(stats)
        val perMinute = actionStats.count { it.timestamp > Date().time - minInMs }
        indicator.setProgress("${File(path).name} ($completed/$actionsCount act, $perMinute act/min)",
                completed.toDouble() / actionsCount)
        if (indicator.isCanceled()) {
            LOG.info("Interpreting actions is canceled by user.")
            return true
        }
        return false
    }
}
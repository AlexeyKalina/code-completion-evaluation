package org.jb.cce.interpretator

import com.intellij.openapi.diagnostic.Logger
import org.jb.cce.InterpretationHandler
import org.jb.cce.actions.Action
import org.jb.cce.actions.ActionStat
import org.jb.cce.util.Progress
import java.io.File
import java.util.*

class InterpretationHandlerImpl(private val indicator: Progress, private val sessionsCount: Int) : InterpretationHandler {
    private companion object {
        val LOG = Logger.getInstance(InterpretationHandlerImpl::class.java)
    }

    private val minInMs = 60000
    private var completed = 0
    private val actionStats = mutableListOf<ActionStat>()

    override fun onActionStarted(action: Action) {
        actionStats.add(ActionStat(action, System.currentTimeMillis()))
    }

    override fun onSessionFinished(path: String): Boolean {
        completed++
        updateProgress(path)
        return isCancelled()
    }

    override fun onFileProcessed(path: String) {
        LOG.info("Interpreting actions for file $path completed. Done: $completed/$sessionsCount")
    }

    override fun onErrorOccurred(error: Throwable, sessionsSkipped: Int) {
        completed += sessionsSkipped
        LOG.error(error)
    }

    override fun isCancelled(): Boolean {
        if (indicator.isCanceled()) {
            LOG.info("Interpreting actions is canceled by user.")
            return true
        }
        return false
    }

    private fun updateProgress(path: String) {
        val perMinute = actionStats.count { it.timestamp > Date().time - minInMs }
        val fileName = File(path).name
        indicator.setProgress(fileName, "$fileName ($completed/$sessionsCount sessions, $perMinute act/min)",
                completed.toDouble() / sessionsCount)
    }
}
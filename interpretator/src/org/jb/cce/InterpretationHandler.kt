package org.jb.cce

import org.jb.cce.actions.ActionStat

interface InterpretationHandler {
    fun invokeOnCompletion(stats: List<ActionStat>, path: String): Boolean
    fun invokeOnFile(sessions: List<Session>, stats: List<ActionStat>, path: String, text: String): Boolean
    fun invokeOnError(error: Throwable)
}
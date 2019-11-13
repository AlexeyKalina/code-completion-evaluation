package org.jb.cce
import org.jb.cce.filter.EvaluationFilter

data class SessionsFilter(val name: String, val filters: Map<String, EvaluationFilter>) {
    companion object {
        val ACCEPT_ALL = SessionsFilter("ALL", emptyMap())
    }

    fun apply(sessions: List<Session>): List<Session> {
        return sessions.filter { session -> filters.all { it.value.shouldEvaluate(session.properties) } }
    }
}
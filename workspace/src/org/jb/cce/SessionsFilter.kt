package org.jb.cce
import org.jb.cce.filter.EvaluationFilter

data class SessionsFilter(val name: String, val filters: MutableMap<String, EvaluationFilter>) {
    fun apply(sessions: List<Session>): List<Session> {
        return sessions.filter { session -> filters.all { it.value.shouldEvaluate(session.properties) } }
    }
}
package org.jb.cce

import com.google.gson.Gson
import org.jb.cce.info.SessionsEvaluationInfo
import java.util.*

class SessionSerializer {
    private val gson = Gson()

    fun serialize(results: SessionsEvaluationInfo): String {
        return gson.toJson(results)
    }

    fun serialize(sessions: List<Session>): String {
        val map = HashMap<UUID, Session>()
        for (session in sessions) {
            map[session.id] = session
        }
        return gson.toJson(map)
    }
}
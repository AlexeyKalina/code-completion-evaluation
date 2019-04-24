package org.jb.cce

import com.google.gson.Gson
import java.util.*

class SessionSerializer {

    fun serialize(sessions: List<Session>) : String {
        val map = HashMap<UUID, Session>()
        for (session in sessions) {
            map[session.id] = session
        }
        return Gson().toJson(map)
    }
}
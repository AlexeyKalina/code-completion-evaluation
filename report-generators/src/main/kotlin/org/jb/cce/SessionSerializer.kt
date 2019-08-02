package org.jb.cce

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.info.SessionsEvaluationInfo
import java.util.*

class SessionSerializer {
    private val gson = GsonBuilder()
            .addDeserializationExclusionStrategy(object : ExclusionStrategy {
                override fun shouldSkipField(f: FieldAttributes) = false
                override fun shouldSkipClass(aClass: Class<*>) = aClass == CompletionPrefix::class.java
            })
            .create()

    fun serialize(results: SessionsEvaluationInfo): String {
        return gson.toJson(results)
    }

    fun serialize(sessions: List<Session>): String {
        val map = HashMap<UUID, Session>()
        for (session in sessions) {
            map[session.id] = session
        }
        return gson.toJson(map)
                .replace("""(\\r|\\n|\\t)""".toRegex(), "")
                .replace("\\\"", "&quot;")
                .replace("★", "*")
    }

    fun deserialize(json: String) : SessionsEvaluationInfo {
        val list = gson.fromJson(json, SessionsEvaluationInfo::class.java)
        return list!!
    }
}
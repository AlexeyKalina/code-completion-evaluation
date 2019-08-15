package org.jb.cce

import com.google.gson.*
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.info.SessionsEvaluationInfo
import java.lang.reflect.Type
import java.util.*

class SessionSerializer {
    companion object {
        private val gson = GsonBuilder()
                .addDeserializationExclusionStrategy(object : ExclusionStrategy {
                    override fun shouldSkipField(f: FieldAttributes) = false
                    override fun shouldSkipClass(aClass: Class<*>) = aClass == CompletionPrefix::class.java
                }).registerTypeAdapter(Suggestion::class.java, object : JsonSerializer<Suggestion> {
                    override fun serialize(src: Suggestion, typeOfSrc: Type, context: JsonSerializationContext): JsonObject {
                        val jsonObject = JsonObject()
                        jsonObject.addProperty("text", src.text)
                        jsonObject.addProperty("presentationText", escapeHtml4(src.presentationText))
                        return jsonObject
                    }
                })
                .create()
    }

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
                .replace("★", "*")
    }

    fun deserialize(json: String) : SessionsEvaluationInfo {
        val list = gson.fromJson(json, SessionsEvaluationInfo::class.java)
        return list!!
    }
}
package org.jb.cce

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionStrategySerializer
import org.jb.cce.info.FileSessionsInfo
import java.lang.reflect.Type
import java.util.*

class SessionSerializer {
    companion object {
        private val gson = GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(Suggestion::class.java, object : JsonSerializer<Suggestion> {
                    override fun serialize(src: Suggestion, typeOfSrc: Type, context: JsonSerializationContext): JsonObject {
                        val jsonObject = JsonObject()
                        jsonObject.addProperty("text", src.text)
                        jsonObject.addProperty("presentationText", escapeHtml4(src.presentationText))
                        return jsonObject
                    }
                })
                .registerTypeAdapter(CompletionStrategy::class.java, CompletionStrategySerializer())
                .create()
    }

    fun serialize(sessions: FileSessionsInfo): String = gson.toJson(sessions)

    fun serialize(sessions: List<Session>): String {
        val map = HashMap<UUID, Session>()
        for (session in sessions) {
            map[session.id] = session
        }
        return gson.toJson(map)
                .replace("""(\\r|\\n|\\t)""".toRegex(), "")
                .replace("★", "*")
    }

    fun deserialize(json: String) : FileSessionsInfo {
        return gson.fromJson(json, FileSessionsInfo::class.java)
    }
}
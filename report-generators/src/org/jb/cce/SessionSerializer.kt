package org.jb.cce

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.info.EvaluationInfo
import org.jb.cce.info.FileEvaluationInfo
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

    fun serialize(sessions: FileEvaluationInfo<Session>): String = gson.toJson(sessions)

    fun serializeConfig(config: EvaluationInfo): String = gson.toJson(config)

    fun serialize(sessions: List<Session>): String {
        val map = HashMap<UUID, Session>()
        for (session in sessions) {
            map[session.id] = session
        }
        return gson.toJson(map)
                .replace("""(\\r|\\n|\\t)""".toRegex(), "")
                .replace("★", "*")
    }

    fun deserialize(json: String) : FileEvaluationInfo<Session> {
        val type = object : TypeToken<FileEvaluationInfo<Session>>() {}.type
        return gson.fromJson(json, type)
    }

    fun deserializeConfig(json: String): EvaluationInfo = gson.fromJson(json, EvaluationInfo::class.java)
}
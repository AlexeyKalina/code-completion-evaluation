package org.jb.cce.actions
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import org.jb.cce.exception.UnexpectedActionException
import org.jb.cce.uast.NodeProperties

class ActionSerializer {

    private val gson = GsonBuilder()
            .addSerializationExclusionStrategy(object : ExclusionStrategy {
                // don't serialize field properties twice
                override fun shouldSkipField(f: FieldAttributes) = f.name == "_properties"
                override fun shouldSkipClass(aClass: Class<*>) = false
            })
            .create()

    fun serialize(actions: FileActions): String {
        return gson.toJson(actions)
    }

    private fun deserialize(action: Map<String, Any>): Action {
        return when (action["type"]) {
            Action.ActionType.MOVE_CARET.name -> MoveCaret((action["offset"] as Double).toInt())
            Action.ActionType.CALL_COMPLETION.name -> CallCompletion(action["prefix"] as String, action["expectedText"] as String,
                    NodeProperties.create((action["nodeProperties"] as Map<String, Any>)["properties"] as Map<String, Any>))
            Action.ActionType.FINISH_SESSION.name -> FinishSession()
            Action.ActionType.PRINT_TEXT.name -> PrintText(action["text"] as String, action["completable"] as Boolean)
            Action.ActionType.DELETE_RANGE.name ->
                DeleteRange((action["begin"] as Double).toInt(), (action["end"] as Double).toInt(), action["completable"] as Boolean)
            else -> throw UnexpectedActionException("Incorrect action type")
        }
    }

    private fun deserialize(actions: List<Map<String, Any>>): List<Action> {
        return actions.asSequence().map { deserialize(it) }.toList()
    }

    fun deserialize(json: String): FileActions {
        val map = gson.fromJson(json, mutableMapOf<String, Any>().javaClass)
        return FileActions(map["path"] as String, map["checksum"] as String,
                (map["sessionsCount"] as Double).toInt(), deserialize(map["actions"] as List<Map<String, Any>>))
    }

    private data class FakeFileActions(val sessionsCount: Int)

    fun getSessionsCount(json: String): Int {
        return gson.fromJson(json, FakeFileActions::class.java).sessionsCount
    }
}
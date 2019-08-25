package org.jb.cce.actions
import com.google.gson.Gson
import org.jb.cce.TokenType
import org.jb.cce.exception.UnexpectedActionException

class ActionSerializer {

    private val gson = Gson()

    private fun serialize(action: Action): String {
        return gson.toJson(action)
    }

    fun serialize(actions: List<Action>): String {
        return actions.asSequence().map { serialize(it) }.joinToString(", ", "[", "]")
    }

    private fun deserialize(action: Map<String, Any>): Action {
        return when (action["type"]) {
            Action.ActionType.MOVE_CARET.name -> MoveCaret((action["offset"] as Double).toInt())
            Action.ActionType.CALL_COMPLETION.name -> CallCompletion(action["prefix"] as String, action["expectedText"] as String,
                    TokenType.valueOf(action["tokenType"] as String))
            Action.ActionType.FINISH_SESSION.name -> FinishSession()
            Action.ActionType.PRINT_TEXT.name -> PrintText(action["text"] as String, action["completable"] as Boolean)
            Action.ActionType.DELETE_RANGE.name ->
            DeleteRange((action["begin"] as Double).toInt(), (action["end"] as Double).toInt(), action["completable"] as Boolean)
            Action.ActionType.OPEN_FILE.name -> OpenFile(action["path"] as String, action["text"] as String)
            else -> throw UnexpectedActionException("Incorrect action type")
        }
    }

    fun deserialize(json: String): List<Action> {
        val list = gson.fromJson(json, mutableListOf<Map<String, Any>>().javaClass)
        return list.asSequence().map { deserialize(it) }.toList()
    }
}

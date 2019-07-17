package org.jb.cce.actions

import org.jb.cce.TokenType

sealed class Action(val type: ActionType) {
    enum class ActionType {
        MOVE_CARET, CALL_COMPLETION, FINISH_SESSION, PRINT_TEXT, DELETE_RANGE, OPEN_FILE
    }
}

data class MoveCaret(val offset: Int) : Action(ActionType.MOVE_CARET)
data class CallCompletion(val prefix: String, val expectedText: String, val tokenType: TokenType) : Action(ActionType.CALL_COMPLETION)
class FinishSession : Action(ActionType.FINISH_SESSION)
data class PrintText(val text: String, val completable: Boolean = false) : Action(ActionType.PRINT_TEXT)
data class DeleteRange(val begin: Int, val end: Int, val completable: Boolean = false) : Action(ActionType.DELETE_RANGE)
data class OpenFile(val path: String, val text: String) : Action(ActionType.OPEN_FILE)


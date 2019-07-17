package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException

class Interpreter(private val invoker: CompletionInvoker) {

    fun interpret(actions: List<Action>, completionType: CompletionType, callbackPerFile: (List<Session>, String, String) -> Unit) {
        if (actions.isEmpty()) return
        val result = mutableListOf<Session>()
        var currentOpenedFilePath = ""
        var currentOpenedFileText = ""
        var session: Session? = null
        var position = 0

        for (action in actions) {
            when (action) {
                is MoveCaret -> {
                    invoker.moveCaret(action.offset)
                    position = action.offset
                }
                is CallCompletion -> {
                    if (session == null) {
                        session = Session(position, action.expectedText, action.tokenType)
                    }
                    session.addLookup(Lookup(action.prefix, invoker.callCompletion(completionType)))
                }
                is CancelSession -> {
                    if (session == null) {
                        throw UnexpectedActionException("Session canceled before created")
                    }
                    result.add(session)
                    session = null
                }
                is PrintText -> invoker.printText(action.text)
                is DeleteRange -> invoker.deleteRange(action.begin, action.end)
                is OpenFile -> {
                    if (!currentOpenedFilePath.isEmpty()) {
                        invoker.closeFile(currentOpenedFilePath)
                        callbackPerFile(result.toList(), currentOpenedFilePath, currentOpenedFileText)
                        result.clear()
                    }
                    invoker.openFile(action.path)
                    currentOpenedFilePath = action.path
                    currentOpenedFileText = action.text
                }
            }
        }
        callbackPerFile(result, currentOpenedFilePath, currentOpenedFileText)
    }
}

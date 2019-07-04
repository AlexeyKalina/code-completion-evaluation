package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import java.io.FileReader
import java.util.function.Consumer

class Interpreter(private val invoker: CompletionInvoker) {

    fun interpret(actions: List<Action>, completionType: CompletionType, callbackPerFile: (List<Session>, String) -> Unit) {
        if (actions.isEmpty()) return
        val result = mutableListOf<Session>()
        var currentOpenedFile = ""
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
                    if (!currentOpenedFile.isEmpty()) {
                        invoker.closeFile(currentOpenedFile)
                        callbackPerFile(result.toList(), currentOpenedFile)
                        result.clear()
                    }
                    invoker.openFile(action.file)
                    currentOpenedFile = action.file
                }
            }
        }
        callbackPerFile(result, currentOpenedFile)
    }
}

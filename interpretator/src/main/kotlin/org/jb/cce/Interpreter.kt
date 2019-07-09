package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import org.omg.CORBA.BooleanHolder

class Interpreter(private val invoker: CompletionInvoker) {

    fun interpret(actions: List<Action>, completionType: CompletionType, callbackPerFile: (List<Session>, String, String, BooleanHolder) -> Unit) {
        if (actions.isEmpty()) return
        val result = mutableListOf<Session>()
        var currentOpenedFilePath = ""
        var currentOpenedFileText = ""
        var session: Session? = null
        var position = 0
        var completionSuccess = false

        iterateActions@
        for (action in actions) {
            when (action) {
                is MoveCaret -> {
                    invoker.moveCaret(action.offset)
                    position = action.offset
                }
                is CallCompletion -> {
                    if (completionType != CompletionType.SMART && completionSuccess) continue@iterateActions
                    if (session == null) {
                        session = Session(position, action.expectedText, action.tokenType)
                    }
                    session.addLookup(Lookup(action.prefix, invoker.callCompletion(completionType, action.expectedText)))
                    completionSuccess = session.lookups.last().suggests.any { it.text == action.expectedText }
                }
                is FinishSession -> {
                    if (session == null) {
                        throw UnexpectedActionException("Session canceled before created")
                    }
                    completionSuccess = false
                    result.add(session)
                    session = null
                }
                is PrintText -> {
                    if (completionType != CompletionType.SMART && action.completable && completionSuccess) continue@iterateActions
                    invoker.printText(action.text)
                }
                is DeleteRange -> {
                    if (completionType != CompletionType.SMART && action.completable && completionSuccess) continue@iterateActions
                    invoker.deleteRange(action.begin, action.end)
                }
                is OpenFile -> {
                    if (!currentOpenedFilePath.isEmpty()) {
                        invoker.closeFile(currentOpenedFilePath)
                        val isCanceled = BooleanHolder()
                        callbackPerFile(result.toList(), currentOpenedFilePath, currentOpenedFileText, isCanceled)
                        if (isCanceled.value) return
                        result.clear()

                    }
                    invoker.openFile(action.path)
                    currentOpenedFilePath = action.path
                    currentOpenedFileText = action.text
                }
            }
        }
        callbackPerFile(result, currentOpenedFilePath, currentOpenedFileText, BooleanHolder())
    }
}

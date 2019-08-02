package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException

class Interpreter(private val invoker: CompletionInvoker) {

    fun interpret(actions: List<Action>, completionType: CompletionType, callbackPerFile: (List<Session>, String, String, Int) -> Boolean) {
        if (actions.isEmpty()) return
        val result = mutableListOf<Session>()
        var currentOpenedFilePath = ""
        var currentOpenedFileText = ""
        var needToClose = false
        var session: Session? = null
        var position = 0
        var completionSuccess = false
        var actionsDone = 0

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
                    session.addLookup(invoker.callCompletion(completionType, action.expectedText, action.prefix))
                    completionSuccess = session.lookups.last().success
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
                    if (completionType == CompletionType.SMART || !action.completable || !completionSuccess)
                        invoker.printText(action.text)
                }
                is DeleteRange -> {
                    if (completionType == CompletionType.SMART || !action.completable || !completionSuccess)
                        invoker.deleteRange(action.begin, action.end)
                }
                is OpenFile -> {
                    if (!currentOpenedFilePath.isEmpty()) {
                        if (needToClose) invoker.closeFile(currentOpenedFilePath)
                        val isCanceled = callbackPerFile(result.toList(), currentOpenedFilePath, currentOpenedFileText, actionsDone)
                        if (isCanceled) return
                        result.clear()
                        actionsDone = 0
                    }
                    needToClose = !invoker.isOpen(action.path)
                    invoker.openFile(action.path)
                    currentOpenedFilePath = action.path
                    currentOpenedFileText = action.text
                }
            }
            actionsDone++
        }
        if (needToClose) invoker.closeFile(currentOpenedFilePath)
        callbackPerFile(result, currentOpenedFilePath, currentOpenedFileText, actionsDone)
    }
}

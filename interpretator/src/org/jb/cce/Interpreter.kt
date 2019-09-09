package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException

class Interpreter {

    fun interpret(invoker: CompletionInvoker, actions: List<Action>, completionType: CompletionType, callbackPerFile: (List<Session>, List<ActionStat>, String, String, Int) -> Boolean) {
        if (actions.isEmpty()) return
        val result = mutableListOf<Session>()
        val stats = mutableListOf<ActionStat>()
        var currentOpenedFilePath = ""
        var currentOpenedFileText = ""
        var needToClose = false
        var session: Session? = null
        var position = 0
        var actionsDone = 0

        iterateActions@
        for (action in actions) {
            stats.add(ActionStat(action, System.currentTimeMillis()))
            when (action) {
                is MoveCaret -> {
                    invoker.moveCaret(action.offset)
                    position = action.offset
                }
                is CallCompletion -> {
                    if (completionType == CompletionType.SMART || session?.success != true) {
                        if (session == null) session = Session(position, action.expectedText, action.tokenType)
                        val completionResult = invoker.callCompletion(action.expectedText, action.prefix, action.isLast)
                        session.addLookup(completionResult.lookup)
                        session.success = completionResult.success
                    }
                }
                is FinishSession -> {
                    if (session == null) {
                        throw UnexpectedActionException("Session canceled before created")
                    }
                    result.add(session)
                    session = null
                }
                is PrintText -> {
                    if (completionType == CompletionType.SMART || !action.completable || session?.success != true)
                        invoker.printText(action.text)
                }
                is DeleteRange -> {
                    if (completionType == CompletionType.SMART || !action.completable || session?.success != true)
                        invoker.deleteRange(action.begin, action.end)
                }
                is OpenFile -> {
                    if (!currentOpenedFilePath.isEmpty()) {
                        if (needToClose) invoker.closeFile(currentOpenedFilePath)
                        val isCanceled = callbackPerFile(result.toList(), stats.toList(), currentOpenedFilePath, currentOpenedFileText, actionsDone)
                        if (isCanceled) return
                        result.clear()
                        stats.clear()
                        actionsDone = 0
                    }
                    needToClose = !invoker.isOpen(action.path)
                    currentOpenedFileText = invoker.openFile(action.path)
                    currentOpenedFilePath = action.path
                }
            }
            actionsDone++
        }
        if (needToClose) invoker.closeFile(currentOpenedFilePath)
        callbackPerFile(result, stats, currentOpenedFilePath, currentOpenedFileText, actionsDone)
    }
}
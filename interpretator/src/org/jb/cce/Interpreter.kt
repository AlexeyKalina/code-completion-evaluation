package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import java.lang.IllegalStateException

class Interpreter {

    fun interpret(invoker: CompletionInvoker, actions: List<Action>, handler: InterpretationHandler) {
        if (actions.isEmpty()) return
        val result = mutableListOf<Session>()
        val stats = mutableListOf<ActionStat>()
        var currentOpenedFilePath = ""
        var currentOpenedFileText = ""
        var needToClose = false
        var isFinished = false
        var skipFile = false
        var session: Session? = null
        var position = 0

        iterateActions@
        for (action in actions) {
            stats.add(ActionStat(action, System.currentTimeMillis()))
            if (skipFile && action !is OpenFile) continue
            when (action) {
                is MoveCaret -> {
                    invoker.moveCaret(action.offset)
                    position = action.offset
                }
                is CallCompletion -> {
                    if (session == null) session = Session(position, action.expectedText, action.tokenType)
                    val completionResult = invoker.callCompletion(action.expectedText, action.prefix)
                    session.addLookup(completionResult.lookup)
                    session.success = completionResult.success
                    val isCanceled = handler.invokeOnCompletion(stats.toList())
                    if (isCanceled) return
                    stats.clear()
                    isFinished = false
                }
                is FinishSession -> {
                    if (session == null) {
                        throw UnexpectedActionException("Session canceled before created")
                    }
                    isFinished = invoker.finishCompletion(session.expectedText, session.lookups.last().text)
                    result.add(session)
                    session = null
                }
                is PrintText -> {
                    if (!action.completable || !isFinished)
                        invoker.printText(action.text)
                }
                is DeleteRange -> {
                    if (!action.completable || !isFinished)
                        invoker.deleteRange(action.begin, action.end)
                }
                is OpenFile -> {
                    skipFile = false
                    if (needToClose) invoker.closeFile(currentOpenedFilePath)
                    needToClose = !invoker.isOpen(action.path)
                    currentOpenedFileText = invoker.openFile(action.path)
                    currentOpenedFilePath = action.path
                    val isCanceled = handler.invokeOnFile(result.toList(), stats.toList(), currentOpenedFilePath, currentOpenedFileText)
                    if (isCanceled) return
                    result.clear()
                    stats.clear()
                    if (action.checksum != computeChecksum(currentOpenedFileText)) {
                        skipFile = true
                        handler.invokeOnError(IllegalStateException("File $currentOpenedFilePath has been modified."))
                    }
                }
            }
        }
        if (needToClose) invoker.closeFile(currentOpenedFilePath)
        handler.invokeOnFile(result, stats, currentOpenedFilePath, currentOpenedFileText)
    }
}
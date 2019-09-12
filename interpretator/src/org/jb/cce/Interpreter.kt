package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import java.lang.IllegalStateException

class Interpreter(private val invoker: CompletionInvoker,
                  private val handler: InterpretationHandler) {

    fun interpret(fileActions: FileActions): List<Session> {
        val sessions = mutableListOf<Session>()
        var isFinished = false
        var session: Session? = null
        var position = 0

        val needToClose = !invoker.isOpen(fileActions.path)
        val text = invoker.openFile(fileActions.path)
        if (fileActions.checksum != computeChecksum(text)) {
            handler.onErrorOccurred(IllegalStateException("File $fileActions.path has been modified."))
            return emptyList()
        }

        iterateActions@
        for (action in fileActions.actions) {
            handler.onActionStarted(action)
            when (action) {
                is MoveCaret -> {
                    invoker.moveCaret(action.offset)
                    position = action.offset
                }
                is CallCompletion -> {
                    if (session == null) session = Session(position, action.expectedText, action.tokenType)
                    val lookup = invoker.callCompletion(action.expectedText, action.prefix)
                    session.addLookup(lookup)
                    isFinished = false
                }
                is FinishSession -> {
                    if (session == null) throw UnexpectedActionException("Session canceled before created")
                    val expectedText = session.expectedText
                    isFinished = invoker.finishCompletion(expectedText, session.lookups.last().text)
                    session.success = session.lookups.last().suggestions.any { it.text == expectedText }
                    sessions.add(session)
                    val isCanceled = handler.onSessionFinished(fileActions.path)
                    if (isCanceled) return sessions
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
            }
        }

        if (needToClose) invoker.closeFile(fileActions.path)
        handler.onFileProcessed(fileActions.path)
        return sessions
    }
}
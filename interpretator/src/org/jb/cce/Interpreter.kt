package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import java.io.File
import java.lang.IllegalStateException
import java.nio.file.Paths

class Interpreter(private val invoker: CompletionInvoker,
                  private val handler: InterpretationHandler,
                  private val projectPath: String?) {

    fun interpret(fileActions: FileActions): List<Session> {
        val sessions = mutableListOf<Session>()
        var isFinished = false
        var session: Session? = null
        var position = 0

        val filePath = if (projectPath == null) fileActions.path else Paths.get(projectPath).resolve(fileActions.path).toString()
        val needToClose = !invoker.isOpen(filePath)
        val text = invoker.openFile(filePath)
        if (fileActions.checksum != computeChecksum(text)) {
            handler.onErrorOccurred(IllegalStateException("File $filePath has been modified."), fileActions.sessionsCount)
            return emptyList()
        }

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
                    val isCanceled = handler.onSessionFinished(filePath)
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

        if (needToClose) invoker.closeFile(filePath)
        if (text != File(filePath).readText()) throw IllegalStateException("Text before and after interpretation doesn't match.")
        handler.onFileProcessed(filePath)
        return sessions
    }
}
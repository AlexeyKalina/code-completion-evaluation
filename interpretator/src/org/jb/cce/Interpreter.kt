package org.jb.cce

import org.jb.cce.FileTextUtil.computeChecksum
import org.jb.cce.FileTextUtil.getDiff
import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import java.nio.file.Paths

class Interpreter(private val invoker: CompletionInvoker,
                  private val handler: InterpretationHandler,
                  private val filter: InterpretFilter,
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
        var shouldCompleteToken = filter.shouldCompleteToken()

        for (action in fileActions.actions) {
            handler.onActionStarted(action)
            when (action) {
                is MoveCaret -> {
                    invoker.moveCaret(action.offset)
                    position = action.offset
                }
                is CallCompletion -> {
                    isFinished = false
                    if (shouldCompleteToken) {
                        if (session == null) session = Session(position, action.expectedText, action.nodeProperties)
                        val lookup = invoker.callCompletion(action.expectedText, action.prefix)
                        session.addLookup(lookup)
                    }
                }
                is FinishSession -> {
                    if (shouldCompleteToken) {
                        if (session == null) throw UnexpectedActionException("Session canceled before created")
                        val expectedText = session.expectedText
                        isFinished = invoker.finishCompletion(expectedText, session.lookups.last().text)
                        session.success = session.lookups.last().suggestions.any { it.text == expectedText }
                        sessions.add(session)
                        val isCanceled = handler.onSessionFinished(filePath)
                        if (isCanceled) return sessions
                        session = null
                    }
                    shouldCompleteToken = filter.shouldCompleteToken()
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

        val resultText = invoker.getText()
        if (text != resultText) {
            invoker.deleteRange(0, resultText.length)
            invoker.printText(text)
            if (needToClose) invoker.closeFile(filePath)
            throw IllegalStateException("Text before and after interpretation doesn't match. Diff:\n${getDiff(text, resultText)}")
        }
        if (needToClose) invoker.closeFile(filePath)
        handler.onFileProcessed(filePath)
        return sessions
    }
}
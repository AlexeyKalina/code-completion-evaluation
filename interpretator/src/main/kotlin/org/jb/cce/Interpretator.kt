package org.jb.cce

import org.jb.cce.actions.Action
import org.jb.cce.actions.CallCompletion
import org.jb.cce.actions.CancelSession
import org.jb.cce.actions.MoveCaret
import org.jb.cce.actions.PrintText
import org.jb.cce.actions.DeleteRange
import org.jb.cce.actions.OpenFile
import org.jb.cce.exception.UnexpectedActionException
import java.io.FileReader
import java.util.function.Consumer

class Interpretator(private val invoker: CompletionInvoker, private val actionScheduler: Consumer<Runnable>) {

    fun interpret(actions: List<Action>, callbackPerFile: Consumer<Triple<List<Session>, String, String>>, callbackFinal: Runnable) {

        val task = object : Runnable {
            val result = mutableListOf<Session>()
            private var currentSession: Session? = null
            private var actionIndex = 0
            private var currentOpenedFile = ""
            private var fileText = ""
            private var currentPosition = 0
            private val batchSize = 30

            override fun run() {
                if (actions.isEmpty()) return

                if(actions.size <= actionIndex) {
                    callbackPerFile.accept(Triple(result, currentOpenedFile, fileText))
                    callbackFinal.run()
                }
                else {
                    val subActions = actions.subList(actionIndex,
                            if (actionIndex + batchSize < actions.size) actionIndex + batchSize else actions.size)
                    processActions(subActions)
                    actionIndex += batchSize
                    actionScheduler.accept(this)
                }
            }

            private fun processActions(actions: List<Action>) {
                var session = currentSession
                var position = currentPosition
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
                            session.addLookup(Lookup(action.text, invoker.callCompletion(action.completionType)))
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
                                callbackPerFile.accept(Triple(result, currentOpenedFile, fileText))
                                result.clear()
                            }
                            fileText = FileReader(action.file).use { it.readText() }
                            invoker.openFile(action.file)
                            currentOpenedFile = action.file
                        }
                    }
                }

                currentSession = session
                currentPosition = position
            }
        }

        actionScheduler.accept(task)
    }
}

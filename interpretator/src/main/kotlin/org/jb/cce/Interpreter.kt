package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import java.io.FileReader
import java.util.function.Consumer

class Interpreter(private val invoker: CompletionInvoker) {

    fun interpret(actions: List<Action>, completionType: CompletionType, callbackPerFile: Consumer<Pair<List<Session>, String>>) {

        val task = object : Runnable {
            val result = mutableListOf<Session>()
            private var currentSession: Session? = null
            private var currentOpenedFile = ""
            private var fileText = ""
            private var currentPosition = 0

            override fun run() {
                if (actions.isEmpty()) return
                processActions(actions)
                callbackPerFile.accept(Pair(result, currentOpenedFile))
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
                                callbackPerFile.accept(Pair(result.toList(), currentOpenedFile))
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

        task.run()
    }
}

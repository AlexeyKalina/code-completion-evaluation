package org.jb.cce.actions

import org.jb.cce.uast.FileNode
import org.jb.cce.visitors.*

class ActionsGenerator(val strategy: CompletionStrategy) {

    fun generate(file: FileNode): List<Action> {
        val deletionVisitor = DeleteMethodBodiesVisitor()
        if (strategy.context == CompletionContext.PREVIOUS) {
            file.accept(deletionVisitor)
        }

        val completionVisitor = when (strategy.statement) {
            CompletionStatement.ALL -> AllCompletableVisitor(file.text, strategy)
            CompletionStatement.METHOD_CALLS -> MethodCallsVisitor(file.text, strategy)
            CompletionStatement.ARGUMENTS -> MethodArgumentsVisitor(file.text, strategy)
            CompletionStatement.VARIABLES -> VariableAccessVisitor(file.text, strategy)
        }

        file.accept(completionVisitor)

        return if (completionVisitor.getActions().isEmpty()) emptyList() else
            listOf(OpenFile(file.path, file.text)) + deletionVisitor.getActions().reversed() + completionVisitor.getActions()
    }
}

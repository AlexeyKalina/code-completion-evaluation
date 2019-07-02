package org.jb.cce.actions

import org.jb.cce.uast.FileNode
import org.jb.cce.visitors.*

fun generateActions(filePath: String, fileText: String, tree: FileNode, strategy: CompletionStrategy): List<Action> {

    val deletionVisitor = DeleteMethodBodiesVisitor()
    if (strategy.context == CompletionContext.PREVIOUS) {
        tree.accept(deletionVisitor)
    }

    val completionVisitor = when (strategy.statement) {
        CompletionStatement.ALL -> AllCompletableVisitor(fileText, strategy)
        CompletionStatement.METHOD_CALLS -> MethodCallsVisitor(fileText, strategy)
        CompletionStatement.ARGUMENTS -> MethodArgumentsVisitor(fileText, strategy)
        CompletionStatement.VARIABLES -> VariableAccessVisitor(fileText, strategy)
    }

    tree.accept(completionVisitor)

    return listOf(OpenFile(filePath)) + deletionVisitor.getActions().reversed() + completionVisitor.getActions()
}

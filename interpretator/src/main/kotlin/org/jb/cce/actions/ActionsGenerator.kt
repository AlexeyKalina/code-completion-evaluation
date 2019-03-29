package org.jb.cce.actions

import org.jb.cce.uast.*

fun generateActions(filePath: String, fileText: String, tree: FileNode, strategy: CompletionStrategy): List<Action> {

    val deletionVisitor = DeleteMethodBodiesVisitor()
    if (strategy.context == CompletionContext.PREVIOUS) {
        deletionVisitor.visit(tree)
    }

    val completionVisitor = when (strategy.statement) {
        CompletionStatement.ALL -> CompletableNodesVisitor(fileText, strategy)
        CompletionStatement.METHOD_CALLS -> MethodCallsVisitor(fileText, strategy)
        CompletionStatement.ARGUMENTS -> MethodArgumentsVisitor(fileText, strategy)
        CompletionStatement.VARIABLES -> VariableAccessVisitor(fileText, strategy)
    }

    completionVisitor.visit(tree)

    return listOf(OpenFile(filePath)) + deletionVisitor.getActions().reversed() + completionVisitor.getActions()
}

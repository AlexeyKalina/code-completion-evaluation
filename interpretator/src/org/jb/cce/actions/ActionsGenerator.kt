package org.jb.cce.actions

import org.jb.cce.FileTextUtil.computeChecksum
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.visitors.*

class ActionsGenerator(val strategy: CompletionStrategy) {

    fun generate(file: TextFragmentNode): FileActions {
        val deletionVisitor = if (strategy.statement == CompletionStatement.ALL_TOKENS) DeleteAllVisitor() else DeleteMethodBodiesVisitor()
        if (strategy.context == CompletionContext.PREVIOUS) file.accept(deletionVisitor)

        val completionVisitor = when (strategy.statement) {
            CompletionStatement.ALL -> AllCompletableVisitor(file.text, strategy, false, file.getOffset())
            CompletionStatement.ALL_STATIC -> AllCompletableVisitor(file.text, strategy, true, file.getOffset())
            CompletionStatement.METHOD_CALLS -> MethodCallsVisitor(file.text, strategy, file.getOffset())
            CompletionStatement.ARGUMENTS -> MethodArgumentsVisitor(file.text, strategy, file.getOffset())
            CompletionStatement.VARIABLES -> VariableAccessVisitor(file.text, strategy, file.getOffset())
            CompletionStatement.ALL_TOKENS -> AllTokensVisitor(file.text, strategy, file.getOffset())
        }

        file.accept(completionVisitor)

        val actions: MutableList<Action> = mutableListOf()
        if (completionVisitor.getGeneratedActions().isNotEmpty()) {
            actions.addAll(deletionVisitor.getActions().reversed())
            actions.addAll(completionVisitor.getGeneratedActions())
        }
        return FileActions(file.path, computeChecksum(file.text), actions.count { it is FinishSession }, actions)
    }
}
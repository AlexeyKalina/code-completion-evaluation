package org.jb.cce.actions

import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.visitors.*

class ActionsGenerator(val strategy: CompletionStrategy) {

    fun generate(file: TextFragmentNode): List<Action> {
        val deletionVisitor = if (strategy.statement == CompletionStatement.ALL_TOKENS) DeleteAllVisitor() else DeleteMethodBodiesVisitor()
        if (strategy.context == CompletionContext.PREVIOUS) file.accept(deletionVisitor)

        val completionVisitor = when (strategy.statement) {
            CompletionStatement.ALL -> AllCompletableVisitor(file.text, strategy)
            CompletionStatement.METHOD_CALLS -> MethodCallsVisitor(file.text, strategy)
            CompletionStatement.ARGUMENTS -> MethodArgumentsVisitor(file.text, strategy)
            CompletionStatement.VARIABLES -> VariableAccessVisitor(file.text, strategy)
            CompletionStatement.ALL_TOKENS -> AllTokensVisitor(file.text, strategy)
        }

        file.accept(completionVisitor)

        val actions: MutableList<Action> = mutableListOf()
        if (completionVisitor.getActions().isNotEmpty()) {
            actions.add(OpenFile(file.path, file.text))
            actions.addAll(deletionVisitor.getActions().reversed())
            actions.addAll(completionVisitor.getActions())

            if (strategy.context == CompletionContext.PREVIOUS && strategy.statement == CompletionStatement.ALL_TOKENS) {
                val lastChildren = file.getChildren().last()
                val lastOffset = lastChildren.getOffset() + lastChildren.getLength()
                actions.add(PrintText(file.text.substring(lastOffset)))
            }
        }

        return actions
    }
}

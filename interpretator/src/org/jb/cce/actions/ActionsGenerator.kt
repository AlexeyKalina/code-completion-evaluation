package org.jb.cce.actions

import org.jb.cce.FileTextUtil.computeChecksum
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.visitors.*

class ActionsGenerator(val strategy: CompletionStrategy) {

    fun generate(file: TextFragmentNode): FileActions {
        val deletionVisitor = if (strategy.completeAllTokens) DeleteAllVisitor() else DeleteMethodBodiesVisitor()
        if (strategy.context == CompletionContext.PREVIOUS) file.accept(deletionVisitor)

        val completionVisitor = if (strategy.completeAllTokens) AllTokensVisitor(file.text, strategy, file.getOffset())
        else CompletableNodesVisitor(file.text, strategy, file.getOffset())

        file.accept(completionVisitor)

        val actions: MutableList<Action> = mutableListOf()
        if (completionVisitor.getGeneratedActions().isNotEmpty()) {
            actions.addAll(deletionVisitor.getActions().reversed())
            actions.addAll(completionVisitor.getGeneratedActions())
        }
        return FileActions(file.path, computeChecksum(file.text), actions.count { it is FinishSession }, actions)
    }
}
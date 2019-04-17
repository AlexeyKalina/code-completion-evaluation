package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.Completable

class CompletableNodesVisitor(override val text: String, strategy: CompletionStrategy): CallCompletionsVisitor(text, strategy) {

    override fun visitCompletable(node: Completable) {
        visitToComplete(node)
    }
}
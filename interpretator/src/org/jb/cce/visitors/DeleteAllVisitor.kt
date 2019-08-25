package org.jb.cce.visitors

import org.jb.cce.actions.DeleteRange
import org.jb.cce.uast.TextFragmentNode

class DeleteAllVisitor : DeletionVisitor() {
    private val actions = mutableListOf<DeleteRange>()

    override fun getActions() = actions

    override fun visitTextFragmentNode(node: TextFragmentNode) {
        actions.add(DeleteRange(node.getOffset(), node.getOffset() + node.getLength()))
    }
}
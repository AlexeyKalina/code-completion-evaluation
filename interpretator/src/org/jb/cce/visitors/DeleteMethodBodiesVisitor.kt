package org.jb.cce.visitors

import org.jb.cce.actions.DeleteRange
import org.jb.cce.uast.statements.declarations.blocks.ClassInitializerNode
import org.jb.cce.uast.statements.declarations.blocks.GlobalNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode

class DeleteMethodBodiesVisitor : DeletionVisitor() {
    private val actions = mutableListOf<DeleteRange>()
    private val bracketSize = 1

    override fun getActions(): List<DeleteRange> = actions

    override fun visitGlobalNode(node: GlobalNode) {
        actions += DeleteRange(node.getOffset() + bracketSize, node.getOffset() + node.getLength() - bracketSize)
    }

    override fun visitClassInitializerNode(node: ClassInitializerNode) {
        actions += DeleteRange(node.getOffset() + bracketSize, node.getOffset() + node.getLength() - bracketSize)
    }

    override fun visitMethodBodyNode(node: MethodBodyNode) {
        actions += DeleteRange(node.getOffset() + bracketSize, node.getOffset() + node.getLength() - bracketSize)
    }
}
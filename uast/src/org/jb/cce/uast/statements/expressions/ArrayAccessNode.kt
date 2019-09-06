package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException

class ArrayAccessNode(offset: Int,
                      length: Int,
                      private val bracketOffset: Int) : ExpressionNode(offset, length), CompositeNode {
    val indices = mutableListOf<ExpressionNode>()
    var prefix: ExpressionNode? = null

    fun addIndex(index: ExpressionNode) {
        indices += index
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node !is ExpressionNode) throw UnifiedAstException("Unexpected child: $node for $this")

        if (bracketOffset > node.getOffset()) this.prefix = node
        else addIndex(node)
    }

    override fun getChildren() = listOfNotNull(prefix) + indices

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitArrayAccessNode(this)
    }
}
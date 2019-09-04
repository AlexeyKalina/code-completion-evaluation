package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.expressions.ExpressionNode

class ArrayAccessNode(name: String,
                      offset: Int,
                      length: Int,
                      isStatic: Boolean = false) : ReferenceNode(name, offset, length, isStatic), CompositeNode {
    private val indices = mutableListOf<ExpressionNode>()

    fun addIndex(index: ExpressionNode) {
        indices += index
    }

    override fun getText() = name

    override fun addChild(node: UnifiedAstNode) {
        if (node !is ExpressionNode) throw UnifiedAstException("Unexpected child: $node for $this")
        else addIndex(node)
    }

    override fun getChildren() = indices

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitArrayAccessNode(this)
    }
}
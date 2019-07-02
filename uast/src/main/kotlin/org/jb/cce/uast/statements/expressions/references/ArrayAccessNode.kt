package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.expressions.ExpressionNode

class ArrayAccessNode(name: String,
                      offset: Int,
                      length: Int) : ReferenceNode(name, offset, length) {

    private val indices = mutableListOf<ExpressionNode>()

    fun addIndex(index: ExpressionNode) {
        indices += index
    }

    override fun getChildren() = indices

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitArrayAccessNode(this)
    }
}
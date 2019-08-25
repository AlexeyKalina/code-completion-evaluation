package org.jb.cce.uast.statements

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.expressions.ExpressionNode
import org.jb.cce.uast.statements.expressions.references.ReferenceNode

class AssignmentNode(offset: Int,
                     length: Int) : StatementNode(offset, length), CompositeNode {

    private lateinit var reference: ReferenceNode
    private lateinit var assigned: ExpressionNode

    fun setReference(reference: ReferenceNode) {
        this.reference = reference
    }

    fun setAssigned(expression: ExpressionNode) {
        assigned = expression
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is ExpressionNode) setAssigned(node)
        else throw UnifiedAstException("Unexpected child: $node for $this")

    }

    override fun getChildren() = listOf(reference, assigned)

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitAssignmentNode(this)
    }
}
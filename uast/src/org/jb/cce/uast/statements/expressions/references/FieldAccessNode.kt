package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.NodeProperties
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.expressions.ExpressionNode

class FieldAccessNode(name: String,
                      offset: Int,
                      length: Int,
                      properties: NodeProperties) : ClassMemberAccessNode(name, offset, length, properties) {
    override fun getChildren(): List<UnifiedAstNode> = listOf()

    override fun getText() = name

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitFieldAccessNode(this)
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is VariableDeclarationNode) return
        if (node !is ExpressionNode) throw UnifiedAstException("Unexpected child: $node for $this")

        if (this.getOffset() > node.getOffset()) this.prefix = node
        else throw UnifiedAstException("Field node $this has child $node with bigger offset")
    }
}

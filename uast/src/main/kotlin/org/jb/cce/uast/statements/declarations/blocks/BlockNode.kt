package org.jb.cce.uast.statements.declarations.blocks

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.declarations.DeclarationNode

abstract class BlockNode(offset: Int,
                length: Int) : UnifiedAstNode(offset, length), CompositeNode {

    private val bodyStatements = mutableListOf<StatementNode>()

    fun addStatement(statement: StatementNode) {
        bodyStatements += statement
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is StatementNode) addStatement(node)
        else throw UnifiedAstException("Unexpected child: $node for $this")
    }

    override fun getChildren(): List<StatementNode> = bodyStatements

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitBlockNode(this)
    }
}
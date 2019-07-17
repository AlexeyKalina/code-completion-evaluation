package org.jb.cce.uast

import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.declarations.DeclarationNode

class FileNode(offset: Int,
               length: Int, val path: String, val text: String) : UnifiedAstNode(offset, length), CompositeNode {
    private val declarations = mutableListOf<DeclarationNode>()
    private val statements = mutableListOf<StatementNode>()

    fun addDeclaration(declaration: DeclarationNode) {
        declarations += declaration
    }

    fun addStatement(statement: StatementNode) {
        statements += statement
    }

    override fun addChild(node: UnifiedAstNode) {
        when (node) {
            is DeclarationNode -> addDeclaration(node)
            is StatementNode -> addStatement(node)
            else -> throw UnifiedAstException("Unexpected child: $node for $this")
        }
    }

    override fun getChildren() = declarations + statements

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitFileNode(this)
    }
}
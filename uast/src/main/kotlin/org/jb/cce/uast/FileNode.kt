package org.jb.cce.uast

import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.declarations.DeclarationNode

class FileNode(offset: Int,
               length: Int) : UnifiedAstNode(offset, length) {
    private val declarations = mutableListOf<DeclarationNode>()
    private val statements = mutableListOf<StatementNode>()

    fun addDeclaration(declaration: DeclarationNode) {
        declarations += declaration
    }

    fun addStatement(statement: StatementNode) {
        statements += statement
    }

    override fun getChildren() = declarations + statements

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitFileNode(this)
    }
}
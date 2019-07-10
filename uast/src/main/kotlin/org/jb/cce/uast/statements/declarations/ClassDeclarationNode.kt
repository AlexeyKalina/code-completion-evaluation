package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.StatementNode

class ClassDeclarationNode(name: String,
                           offset: Int,
                           length: Int) : DeclarationNode(name, offset, length) {

    private val members = mutableListOf<DeclarationNode>()
    private val statements = mutableListOf<StatementNode>()

    fun addMember(member: DeclarationNode) {
        members += member
    }

    fun addStatement(statement: StatementNode) {
        statements += statement
    }

    override fun getChildren() = members

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitClassDeclarationNode(this)
    }
}
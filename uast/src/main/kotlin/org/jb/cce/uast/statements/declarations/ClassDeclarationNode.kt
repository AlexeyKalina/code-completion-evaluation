package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.UnifiedAstVisitor

class ClassDeclarationNode(name: String,
                           offset: Int,
                           length: Int) : DeclarationNode(name, offset, length) {

    private val members = mutableListOf<DeclarationNode>()

    fun addMember(member: DeclarationNode) {
        members += member
    }

    override fun getChildren() = members

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitClassDeclarationNode(this)
    }
}
package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.StatementNode

class ClassDeclarationNode(name: String,
                           offset: Int,
                           length: Int) : DeclarationNode(name, offset, length), CompositeNode {

    private val members = mutableListOf<DeclarationNode>()

    fun addMember(member: DeclarationNode) {
        members += member
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is DeclarationNode) addMember(node)
        else throw UnifiedAstException("Unexpected child: $node for $this")
    }

    override fun getChildren() = members

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitClassDeclarationNode(this)
    }
}
package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.EvaluationRoot
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.expressions.references.TypeReferenceNode

class ClassDeclarationNode(private val header: ClassHeaderNode,
                           offset: Int,
                           length: Int) : DeclarationNode(header.getName(), offset, length), CompositeNode, EvaluationRoot {

    private val members = mutableListOf<DeclarationNode>()

    fun addMember(member: DeclarationNode) {
        members += member
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is TypeReferenceNode) return // In case of anonymous class
        if (node is DeclarationNode) addMember(node)
        else throw UnifiedAstException("Unexpected child: $node for $this")
    }

    override fun getChildren() = members

    override fun getName() = header.getName()

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitClassDeclarationNode(this)
    }

    override fun contains(offset: Int) = offset in header.getOffset() .. header.getOffset() + header.getLength()
}
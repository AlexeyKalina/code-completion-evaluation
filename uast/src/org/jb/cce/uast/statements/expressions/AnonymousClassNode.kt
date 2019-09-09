package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode

class AnonymousClassNode(offset: Int, length: Int): ExpressionNode(offset, length), CompositeNode {
    private var instanceCreationCall: MethodCallNode? = null
    private var classDeclaration: ClassDeclarationNode? = null

    override fun addChild(node: UnifiedAstNode) {
        when (node) {
            is MethodCallNode -> instanceCreationCall = node
            is ClassDeclarationNode -> classDeclaration = node
            else -> throw UnifiedAstException("Unexpected child: $node for $this")
        }
    }

    override fun getChildren(): List<UnifiedAstNode> =  listOfNotNull(instanceCreationCall, classDeclaration)

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitAnonymousClassNode(this)
    }
}
package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.EvaluationRoot
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode

class MethodDeclarationNode(offset: Int,
                            length: Int) : DeclarationNode("", offset, length), EvaluationRoot {

    private var header: MethodHeaderNode? = null
    private var body: MethodBodyNode? = null

    fun setHeader(header: MethodHeaderNode) {
        this.header = header
    }

    fun setBody(body: MethodBodyNode) {
        this.body = body
    }

    override fun getName() = if (header != null) header!!.getName() else "<no_name>"

    override fun getChildren() = listOfNotNull(header, body)

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitMethodDeclarationNode(this)
    }

    override fun contains(offset: Int): Boolean {
        val headerNode = header
        return headerNode != null && offset in headerNode.getOffset() .. headerNode.getOffset() + headerNode.getLength()
    }
}
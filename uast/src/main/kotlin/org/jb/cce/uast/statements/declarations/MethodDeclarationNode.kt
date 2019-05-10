package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode

class MethodDeclarationNode(offset: Int,
                            length: Int) : DeclarationNode("", offset, length) {

    private var header: MethodHeaderNode? = null
    private var body: MethodBodyNode? = null

    fun setHeader(header: MethodHeaderNode) {
        this.header = header
    }

    fun setBody(body: MethodBodyNode) {
        this.body = body
    }

    override fun getName() = if (header != null) header!!.getName() else ""

    override fun getChildren() = (header?.let { listOf(it) } ?: listOf()) + (body?.let { listOf(it) } ?: listOf())
}
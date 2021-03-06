package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.UnifiedAstVisitor

class MethodHeaderNode(name: String,
                       offset: Int,
                       length: Int) : DeclarationNode(name, offset, length) {

    private val arguments = mutableListOf<VariableDeclarationNode>()

    fun addArgument(argument: VariableDeclarationNode) {
        arguments += argument
    }

    override fun getChildren() = arguments

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitMethodHeaderNode(this)
    }
}
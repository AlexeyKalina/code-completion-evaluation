package org.jb.cce.uast.util

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstRecursiveVisitor
import org.jb.cce.uast.statements.declarations.DeclarationNode
import org.jb.cce.uast.statements.expressions.NamedNode

class UastPrinter : UnifiedAstRecursiveVisitor() {
    private var level = 0
    private val builder = StringBuilder()
    override fun visit(node: UnifiedAstNode) {
        printNode(node, node::class.java.simpleName)
    }

    override fun visitDeclarationNode(node: DeclarationNode) {
        printNode(node, "${node::class.java.simpleName} (${node.getName()})")
    }

    override fun visitNamedNode(node: NamedNode) {
        printNode(node, "${node::class.java.simpleName} (${node.name})")
    }

    private fun printNode(node: UnifiedAstNode, text: String) {
        builder.append("  ".repeat(level))
        builder.appendln(text)
        level += 1
        super.visitChildren(node)
        level -= 1
    }

    fun getText(): String {
        return builder.toString()
    }
}
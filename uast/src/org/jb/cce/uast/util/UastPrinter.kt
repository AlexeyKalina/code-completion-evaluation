package org.jb.cce.uast.util

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstRecursiveVisitor
import org.jb.cce.uast.statements.declarations.DeclarationNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.references.ReferenceNode

class UastPrinter : UnifiedAstRecursiveVisitor() {
    private var level = 0
    private var isPrefix = false
    private var isArgument = false
    private val builder = StringBuilder()
    override fun visit(node: UnifiedAstNode) {
        printNode(node, node::class.java.simpleName)
    }

    override fun visitDeclarationNode(node: DeclarationNode) {
        printNode(node, "${node::class.java.simpleName} (${node.getName()})")
    }

    override fun visitNamedNode(node: NamedNode) {
        val postfix =  when {
            isPrefix -> " - prefix"
            isArgument -> " - argument"
            else -> ""
        }
        val prevValueArg = isArgument
        isArgument = true
        printNode(node, "${node::class.java.simpleName} (${node.name})" + postfix)
        isArgument = prevValueArg
    }

    override fun visitReferenceNode(node: ReferenceNode) {
        super.visitReferenceNode(node)
        val prefix = node.prefix
        if (prefix != null) {
            val prevValuePrefix = isPrefix
            isPrefix = true
            level += 1
            prefix.accept(this)
            level -= 1
            isPrefix = prevValuePrefix
        }
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
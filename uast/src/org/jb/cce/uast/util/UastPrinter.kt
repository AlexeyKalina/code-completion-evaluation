package org.jb.cce.uast.util

import org.jb.cce.uast.Completable
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstRecursiveVisitor
import org.jb.cce.uast.statements.declarations.DeclarationNode
import org.jb.cce.uast.statements.expressions.ArrayAccessNode
import org.jb.cce.uast.statements.expressions.ExpressionNode
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

    override fun visitExpressionNode(node: ExpressionNode) {
        val info = getInfo(node)
        isPrefix = false
        isArgument = false
        printNode(node, "${node::class.java.simpleName}$info")
    }

    override fun visitNamedNode(node: NamedNode) {
        val info = getInfo(node)
        val prevValueArg = isArgument
        isArgument = true
        printNode(node, "${node::class.java.simpleName} (${node.name})$info")
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

    override fun visitArrayAccessNode(node: ArrayAccessNode) {
        builder.append("  ".repeat(level))
        builder.appendln(node::class.java.simpleName)
        level += 1
        val prevValuePrefix = isPrefix
        isPrefix = true
        node.prefix?.accept(this)
        isPrefix = prevValuePrefix
        val prevValueArg = isArgument
        isArgument = true
        node.indices.forEach { it.accept(this) }
        isArgument = prevValueArg
        level -= 1
    }

    private fun printNode(node: UnifiedAstNode, text: String) {
        builder.append("  ".repeat(level))
        builder.appendln(text)
        level += 1
        super.visitChildren(node)
        level -= 1
    }

    private fun getInfo(node: UnifiedAstNode): String {
        var info = when {
            isPrefix -> "prefix"
            isArgument -> "argument"
            else -> ""
        }
        if (node is Completable) {
            if (info.isNotBlank()) info += "; "
            info += " ${node.getProperties().properties}"
        }
        return if (info.isNotBlank()) " - $info" else ""
    }

    fun getText(): String {
        return builder.toString()
    }
}
package org.jb.cce.psi

import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.util.ConverterHelper
import java.util.*

class PsiPythonVisitor: PyRecursiveElementVisitor() {
    private var _file: FileNode? = null
    var file: FileNode
        get() = _file ?: throw PsiConverterException("Invoke 'apply' with visitor on Python PSI first")
        private set(value) { _file = value }

    private val stackOfNodes: Deque<UnifiedAstNode> = ArrayDeque<UnifiedAstNode>()

    override fun visitPyFile(node: PyFile?) {
        if (node == null) return
        file = FileNode(node.textOffset, node.textLength)
        stackOfNodes.addLast(file)
        super.visitPyFile(node)
        stackOfNodes.removeLast()
    }

    override fun visitPyCallExpression(node: PyCallExpression?) {
        val callee = node?.callee ?: return
        val methodCall = visitReferenceExpression(callee) { name, offset, length -> MethodCallNode(name, offset, length) }
        stackOfNodes.addLast(methodCall)
        super.visitPyArgumentList(node.argumentList)
        stackOfNodes.removeLast()
    }

    override fun visitPyElement(node: PyElement?) {
        if (node == null) return
        if (node is PyReferenceExpression) {
            visitReferenceExpression(node) { name, offset, length -> VariableAccessNode(name, offset, length) }
            super.visitPyElement(node)
        } else
            super.visitPyElement(node)
    }

    private fun <T: NamedNode> visitReferenceExpression(node: PyElement, factory : (String, Int, Int) -> T): T {
        val namedNode = when (node) {
            is PyReferenceExpressionImpl -> factory(node.nameElement?.text ?: throw PsiConverterException("Empty name"),
                    node.nameElement?.startOffset ?: throw PsiConverterException("Empty offset"),
                    node.nameElement?.textLength ?: throw PsiConverterException("Empty name length"))
            else -> factory(node.name ?: throw PsiConverterException("Empty name"),
                    node.textOffset, node.name?.length ?: throw PsiConverterException("Empty name length"))
        }
        addToParent(namedNode)
        return namedNode
    }

    override fun visitPyFunction(node: PyFunction?) {
        if (node == null) return
        val function = MethodDeclarationNode(node.textOffset, node.textLength)
        val body = MethodBodyNode(node.statementList.textOffset, node.textLength)
        function.setBody(body)
        addToParent(function)
        stackOfNodes.addLast(body)
        super.visitPyFunction(node)
        stackOfNodes.removeLast()
    }

    override fun visitPyClass(node: PyClass?) {
        if (node == null) return
        val classNode = ClassDeclarationNode(node.name ?: "", node.textOffset, node.textLength)
        addToParent(classNode)
        stackOfNodes.addLast(classNode)
        super.visitPyClass(node)
        stackOfNodes.removeLast()
    }

    private fun addToParent(node: UnifiedAstNode) {
        ConverterHelper.addToParent(node, stackOfNodes.last)
    }
}
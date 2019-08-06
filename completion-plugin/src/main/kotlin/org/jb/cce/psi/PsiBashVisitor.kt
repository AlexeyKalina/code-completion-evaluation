package org.jb.cce.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.sh.psi.ShFunctionDefinition
import com.intellij.sh.psi.ShSimpleCommand
import com.intellij.sh.psi.ShVariable
import com.intellij.sh.psi.ShVisitor
import com.intellij.sh.psi.impl.ShLazyBlockImpl
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodHeaderNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import java.util.*

class PsiBashVisitor(private val path: String, private val text: String): ShVisitor(), PsiRecursiveVisitor {
    private var _file: FileNode? = null
    var file: FileNode
        get() = _file ?: throw PsiConverterException("Invoke 'accept' with visitor on Bash PSI first")
        private set(value) { _file = value }

    private val stackOfNodes: Deque<UnifiedAstNode> = ArrayDeque<UnifiedAstNode>()

    override fun visitFile(node: PsiFile?) {
        file = FileNode(node?.textOffset ?: 0, node?.textLength ?: 0, path, text)
        stackOfNodes.addLast(file)
        super.visitFile(node)
    }

    override fun visitElement(element: PsiElement) {
        if (element is ShLazyBlockImpl) visitMethodBodyNode(element)
        super.visitElement(element)
        element.acceptChildren(this)
    }

    override fun visitSimpleCommand(o: ShSimpleCommand) {
        try {
            val callee = o.command
            val methodCall = MethodCallNode(callee.text, callee.textOffset, callee.textLength)
            addToParent(methodCall)

            stackOfNodes.addLast(methodCall)
            super.visitSimpleCommand(o)
            stackOfNodes.removeLast()
        } catch (e: Exception) {
            return
        }
    }

    override fun visitVariable(o: ShVariable) {
        val text = o.text
        val variableAccess = if (text.startsWith('$'))
            VariableAccessNode(o.text.substring(1), o.textOffset + 1, o.textLength - 1)
            else VariableAccessNode(o.text, o.textOffset, o.textLength)
        addToParent(variableAccess)
        super.visitVariable(o)
    }

    override fun visitFunctionDefinition(node: ShFunctionDefinition) {
        val function = MethodDeclarationNode(node.textOffset, node.textLength)
        val header = MethodHeaderNode(node.function?.text ?: "<empty_name>", node.textOffset, node.textLength)
        function.setHeader(header)
        addToParent(function)
        stackOfNodes.addLast(function)
        super.visitFunctionDefinition(node)
        stackOfNodes.removeLast()
    }

    private fun visitMethodBodyNode(node: ShLazyBlockImpl) {
        val lastNode = stackOfNodes.last as? MethodDeclarationNode ?: return visitElement(node)
        val body = MethodBodyNode(node.textOffset, node.textLength)
        lastNode.setBody(body)
        stackOfNodes.addLast(body)
        visitElement(node)
        stackOfNodes.removeLast()
    }

    private fun addToParent(node: UnifiedAstNode) {
        val parentNode = stackOfNodes.last
        if (parentNode is CompositeNode) parentNode.addChild(node)
        else throw UnifiedAstException("Unexpected parent $parentNode for node $node")
    }
}
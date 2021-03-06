package org.jb.cce.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.sh.psi.*
import com.intellij.sh.psi.impl.ShLazyBlockImpl
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.*
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.NodeProperties
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodHeaderNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import java.util.*

class PsiBashVisitor(private val path: String, private val text: String): ShVisitor(), PsiRecursiveVisitor, PsiVisitor {
    private var _file: FileNode? = null
    private val stackOfNodes: Deque<UnifiedAstNode> = ArrayDeque<UnifiedAstNode>()

    override fun getFile(): FileNode = _file ?: throw PsiConverterException("Invoke 'accept' with visitor on Bash PSI first")

    override fun visitFile(node: PsiFile) {
        _file = FileNode(node.textOffset, node.textLength, path, text)
        stackOfNodes.addLast(_file)
        super.visitFile(node)
    }

    override fun visitElement(element: PsiElement) {
        if (element is ShLazyBlockImpl) return visitMethodBodyNode(element)
        super.visitElement(element)
        element.acceptChildren(this)
    }

    override fun visitGenericCommandDirective(node: ShGenericCommandDirective) {
        if (node.text.contains('$')) return super.visitGenericCommandDirective(node)

        val methodCall = MethodCallNode(node.text, node.textOffset, node.textLength, getNodeProperties(TypeProperty.METHOD_CALL))
        addToParent(methodCall)

        stackOfNodes.addLast(methodCall)
        super.visitGenericCommandDirective(node)
        stackOfNodes.removeLast()
    }

    override fun visitVariable(o: ShVariable) {
        val text = o.text
        val properties = getNodeProperties(TypeProperty.VARIABLE)
        val variableAccess = if (text.startsWith('$'))
            VariableAccessNode(o.text.substring(1), o.textOffset + 1, o.textLength - 1, properties)
            else VariableAccessNode(o.text, o.textOffset, o.textLength, properties)
        addToParent(variableAccess)
        super.visitVariable(o)
    }

    override fun visitFunctionDefinition(node: ShFunctionDefinition) {
        val function = MethodDeclarationNode(node.textOffset, node.textLength)
        val nameNode = node.function
        val header = MethodHeaderNode(nameNode?.text ?: "<empty_name>",
                nameNode?.textOffset ?: throw PsiConverterException("Empty name length"), nameNode.textLength)
        function.setHeader(header)
        addToParent(function)
        stackOfNodes.addLast(function)
        super.visitFunctionDefinition(node)
        stackOfNodes.removeLast()
    }

    private fun visitMethodBodyNode(node: ShLazyBlockImpl) {
        val lastNode = stackOfNodes.last as? MethodDeclarationNode ?: return node.acceptChildren(this)
        val body = MethodBodyNode(node.textOffset, node.textLength)
        lastNode.setBody(body)
        stackOfNodes.addLast(body)
        node.acceptChildren(this)
        stackOfNodes.removeLast()
    }

    private fun addToParent(node: UnifiedAstNode) {
        val parentNode = stackOfNodes.last
        if (parentNode is CompositeNode) parentNode.addChild(node)
        else throw UnifiedAstException("Unexpected parent $parentNode for node $node")
    }

    private fun getNodeProperties(tokenType: TypeProperty): NodeProperties {
        val properties = NodeProperties()
        properties.tokenType = tokenType
        return properties
    }
}
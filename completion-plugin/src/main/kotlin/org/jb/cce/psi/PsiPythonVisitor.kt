package org.jb.cce.psi

import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodHeaderNode
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import java.util.*
import java.util.logging.Logger

class PsiPythonVisitor(private val path: String, private val text: String): PyRecursiveElementVisitor() {
    private val LOG = Logger.getLogger(this.javaClass.name)!!
    private var _file: FileNode? = null

    var file: FileNode
        get() = _file ?: throw PsiConverterException("Invoke 'accept' with visitor on Python PSI first")
        private set(value) { _file = value }

    private val stackOfNodes: Deque<UnifiedAstNode> = ArrayDeque<UnifiedAstNode>()
    private val stackOfDeclarations: Deque<VariableDeclarationNode> = ArrayDeque<VariableDeclarationNode>()
    private val stackOfLevelDeclarationCounts: Deque<Int> = ArrayDeque<Int>()

    override fun visitPyFile(node: PyFile) {
        file = FileNode(node.textOffset, node.textLength, path, text)
        stackOfNodes.addLast(file)
        stackOfLevelDeclarationCounts.addLast(0)
        super.visitPyFile(node)
        clearLastStackLevel()
    }

    override fun visitPyCallExpression(node: PyCallExpression) {
        val callee = node.callee
        if (callee == null) {
            LOG.warning("Empty callee for PyCallExpression: ${node.text}")
        }

        if (callee !is PyReferenceExpression) {
            super.visitPyCallExpression(node)
            return
        }
        val methodCall = visitReferenceExpression(callee) { name, offset, length -> MethodCallNode(name, offset, length) }
        stackOfNodes.addLast(methodCall)
        super.visitPyArgumentList(node.argumentList)
        stackOfNodes.removeLast()
    }

    override fun visitPyTargetExpression(node: PyTargetExpression) {
        if (stackOfDeclarations.any { it.getName() == node.name }) {
            val accessNode = VariableAccessNode(node.name ?: throw PsiConverterException("Empty name"), node.textOffset, node.textLength)
            addToParent(accessNode)
        } else {
            val declarationNode = VariableDeclarationNode(node.name ?: throw PsiConverterException("Empty name"), node.textOffset, node.textLength)
            addToParent(declarationNode)
            stackOfNodes.addLast(declarationNode)
            stackOfDeclarations.addLast(declarationNode)
            stackOfLevelDeclarationCounts.addLast(stackOfLevelDeclarationCounts.removeLast() + 1)
            super.visitPyTargetExpression(node)
            stackOfNodes.removeLast()
        }
    }

    override fun visitPyImportStatement(node: PyImportStatement) {
    }

    override fun visitPyElement(node: PyElement) {
        if (node is PyReferenceExpression) {
            visitReferenceExpression(node) { name, offset, length -> VariableAccessNode(name, offset, length) }
        }
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

    override fun visitPyFunction(node: PyFunction) {
        val function = MethodDeclarationNode(node.textOffset, node.textLength)
        val header = MethodHeaderNode(node.name ?: "<empty_name>", node.textOffset, node.textLength)
        val body = MethodBodyNode(node.statementList.textOffset, node.statementList.textLength)
        function.setHeader(header)
        function.setBody(body)
        addToParent(function)
        stackOfNodes.addLast(body)
        stackOfLevelDeclarationCounts.addLast(0)
        super.visitPyFunction(node)
        clearLastStackLevel()
    }

    override fun visitPyClass(node: PyClass) {
        val classNode = ClassDeclarationNode(node.name ?: "", node.textOffset, node.textLength)
        addToParent(classNode)
        stackOfNodes.addLast(classNode)
        stackOfLevelDeclarationCounts.addLast(0)
        super.visitPyStatementList(node.statementList)
        clearLastStackLevel()
    }

    private fun addToParent(node: UnifiedAstNode) {
        val parentNode = stackOfNodes.last
        if (parentNode is CompositeNode) parentNode.addChild(node)
        else throw UnifiedAstException("Unexpected parent $parentNode for node $node")
    }

    private fun clearLastStackLevel() {
        val count = stackOfLevelDeclarationCounts.removeLast()
        for (i in 1..count) stackOfDeclarations.removeLast()
        stackOfNodes.removeLast()
    }
}
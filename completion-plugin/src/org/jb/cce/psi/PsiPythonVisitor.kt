package org.jb.cce.psi

import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.*
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.declarations.*
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import java.util.*
import java.util.logging.Logger

class PsiPythonVisitor(private val path: String, private val text: String): PyRecursiveElementVisitor(), PsiVisitor {
    private val LOG = Logger.getLogger(this.javaClass.name)!!
    private var _file: FileNode? = null

    override fun getFile(): FileNode = _file ?: throw PsiConverterException("Invoke 'accept' with visitor on Bash PSI first")

    private val stackOfNodes: Deque<UnifiedAstNode> = ArrayDeque<UnifiedAstNode>()
    private val stackOfDeclarations: Deque<VariableDeclarationNode> = ArrayDeque<VariableDeclarationNode>()
    private val stackOfLevelDeclarationCounts: Deque<Int> = ArrayDeque<Int>()
    private var isArgument = false

    override fun visitPyFile(node: PyFile) {
        _file = FileNode(node.textOffset, node.textLength, path, text)
        stackOfNodes.addLast(_file)
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
        val methodCall = visitReferenceExpression(callee) { name, offset, length ->
            MethodCallNode(name, offset, length, NodeProperties(TypeProperty.METHOD_CALL, isArgument, false, ""))
        }
        stackOfNodes.addLast(methodCall)
        val prevValue = isArgument
        isArgument = true
        super.visitPyArgumentList(node.argumentList)
        isArgument = prevValue
        stackOfNodes.removeLast()
    }

    override fun visitPyTargetExpression(node: PyTargetExpression) {
        if (stackOfDeclarations.any { it.getName() == node.nameElement?.text }) {
            val accessNode = VariableAccessNode(node.nameElement?.text ?: throw PsiConverterException("Empty name"),
                    node.nameElement?.startOffset ?: throw PsiConverterException("Empty offset"),
                    node.nameElement?.textLength ?: throw PsiConverterException("Empty name length"),
                    NodeProperties(TypeProperty.VARIABLE, isArgument, false, ""))
            addToParent(accessNode)
        } else {
            val declarationNode = VariableDeclarationNode(node.nameElement?.text ?: throw PsiConverterException("Empty name"),
                    node.nameElement?.startOffset ?: throw PsiConverterException("Empty offset"),
                    node.nameElement?.textLength ?: throw PsiConverterException("Empty name length"))
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
            visitReferenceExpression(node) {
                name, offset, length -> VariableAccessNode(name, offset, length, NodeProperties(TypeProperty.VARIABLE, isArgument, false, ""))
            }
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
        val nameNode = node.nameNode
        val header = MethodHeaderNode(nameNode?.text ?: "<empty_name>",
                nameNode?.startOffset ?: throw PsiConverterException("Empty name offset"), nameNode.textLength)
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
        val nameNode = node.nameNode
        val header = if (nameNode != null) ClassHeaderNode(nameNode.text, nameNode.startOffset, nameNode.textLength)
            else ClassHeaderNode("<no_name>", node.textOffset, 0)
        val classNode = ClassDeclarationNode(header, node.textOffset, node.textLength)
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
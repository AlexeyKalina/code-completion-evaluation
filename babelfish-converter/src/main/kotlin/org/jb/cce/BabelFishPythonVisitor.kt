package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.CompletableNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.AssignmentNode
import org.jb.cce.uast.statements.declarations.blocks.BlockNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.references.VariableAccessNode

class BabelFishPythonVisitor: BabelFishUnifiedVisitor() {

    override fun visitFileNode(json: JsonObject): FileNode {
        val file = FileNode(-1, -1)
        visitChildren(json, file)
        return file
    }

    override fun visitChild(json: JsonObject, parentNode: UnifiedAstNode) {
        when {
            typeEquals(json, "python:Call") -> visitMethodCall(json, parentNode)
            (typeEquals(json, "python:BoxedValue") || typeEquals(json, "python:QualifiedIdentifier")) ||
                    roleExists(json, "Argument") -> visitVariableAccess(json, parentNode)
            else -> super.visitChild(json, parentNode)
        }
    }

    override fun visitVariableAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableAccessNodes = visitCompletableNodes(json) { name, offset, length -> VariableAccessNode(name, offset, length) }
        for (node in variableAccessNodes) {
            when (parentNode) {
                is BlockNode -> parentNode.addStatement(node)
                is AssignmentNode -> parentNode.setAssigned(node)
                is MethodCallNode -> parentNode.addArgument(node)
                is FileNode -> parentNode.addStatement(node)
                else -> throw UnifiedAstException("Unexpected parent for variable access node")
            }
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val funcObj = json["func"].asJsonObject
        val methodCallNodes = visitCompletableNodes(funcObj) { name, offset, length -> MethodCallNode(name, offset, length) }
        for (node in methodCallNodes) {
            when (parentNode) {
                is BlockNode -> parentNode.addStatement(node)
                is AssignmentNode -> parentNode.setAssigned(node)
                is MethodCallNode -> parentNode.addArgument(node)
                is FileNode -> parentNode.addStatement(node)
                else -> throw UnifiedAstException("Unexpected parent for method call node")
            }
        }
        val lastNode = methodCallNodes.last()

        visitChildren(json, lastNode)
    }

    override fun <T: CompletableNode> visitCompletableNodes (json: JsonObject, factory : (String, Int, Int) -> T): List<CompletableNode> {
        val nodes = mutableListOf<T>()
        when {
            typeEquals(json, "python:QualifiedIdentifier") -> return visitPythonQualifiedIdentifier(json, factory)
            typeEquals(json, "python:BoxedName") -> nodes.add(visitBoxedName(json, factory))
            else -> throw UnifiedAstException("Unexpected completable node")
        }
        return nodes
    }

    private fun <T: CompletableNode> visitPythonQualifiedIdentifier(json: JsonObject, factory : (String, Int, Int) -> T): List<CompletableNode> {
        val nodes = mutableListOf<CompletableNode>()
        val names = json["identifiers"].asJsonArray
        for (i in 0..names.size()-2) {
            nodes.add(visitBoxedName(names[i].asJsonObject) { name, offset, length -> VariableAccessNode(name, offset, length) })
        }
        val lastName = names.last().asJsonObject
        nodes.add(visitBoxedName(lastName, factory))
        return nodes

    }

    private fun <T: CompletableNode> visitBoxedName(json: JsonObject, factory : (String, Int, Int) -> T): T {
        val boxedValue = json["boxed_value"].asJsonObject
        return factory(visitIdentifier(boxedValue), getOffset(boxedValue), getLength(boxedValue))
    }
}
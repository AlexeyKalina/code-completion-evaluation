package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.Completable
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode

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
            addToParent(node as UnifiedAstNode, parentNode)
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val funcObj = json["func"].asJsonObject
        val methodCallNodes = visitCompletableNodes(funcObj) { name, offset, length -> MethodCallNode(name, offset, length) }
        for (node in methodCallNodes) {
            addToParent(node as UnifiedAstNode, parentNode)
        }
        val lastNode = methodCallNodes.last()

        visitChildren(json, lastNode as UnifiedAstNode)
    }

    override fun <T: Completable> visitCompletableNodes (json: JsonObject, factory : (String, Int, Int) -> T): List<Completable> {
        val nodes = mutableListOf<T>()
        when {
            typeEquals(json, "python:QualifiedIdentifier") -> return visitPythonQualifiedIdentifier(json, factory)
            typeEquals(json, "python:BoxedName") -> nodes.add(visitBoxedName(json, factory))
            else -> throw UnifiedAstException("Unexpected completable node")
        }
        return nodes
    }

    private fun <T: Completable> visitPythonQualifiedIdentifier(json: JsonObject, factory : (String, Int, Int) -> T): List<Completable> {
        val nodes = mutableListOf<Completable>()
        val names = json["identifiers"].asJsonArray
        for (i in 0 until (names.size() - 1)) {
            nodes.add(visitBoxedName(names[i].asJsonObject) { name, offset, length -> VariableAccessNode(name, offset, length) })
        }
        val lastName = names.last().asJsonObject
        nodes.add(visitBoxedName(lastName, factory))
        return nodes

    }

    private fun <T: Completable> visitBoxedName(json: JsonObject, factory : (String, Int, Int) -> T): T {
        val boxedValue = json["boxed_value"].asJsonObject
        return factory(visitIdentifier(boxedValue), getOffset(boxedValue), getLength(boxedValue))
    }
}
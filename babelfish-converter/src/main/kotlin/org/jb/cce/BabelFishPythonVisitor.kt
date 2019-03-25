package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.CompletableNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
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
                    roleExists(json, "Argument") -> visitFieldAccess(json, parentNode)
            else -> super.visitChild(json, parentNode)
        }
    }

    override fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableAccess = VariableAccessNode(visitQualifier(json), getOffset(json), getLength(json))

        when (parentNode) {
            is BlockNode -> parentNode.addStatement(variableAccess)
            is AssignmentNode -> parentNode.setAssigned(variableAccess)
            is MethodCallNode -> parentNode.addArgument(variableAccess)
            is FileNode -> parentNode.addStatement(variableAccess)
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val funcObj = json["func"].asJsonObject
        val methodCall = MethodCallNode(visitQualifier(funcObj), getOffset(json), getLength(json))

        visitChildren(json, methodCall)

        when (parentNode) {
            is BlockNode -> parentNode.addStatement(methodCall)
            is MethodCallNode -> parentNode.addArgument(methodCall)
            is FileNode -> parentNode.addStatement(methodCall)
        }
    }

    private fun visitQualifier(json: JsonObject): CompletableNode {
        return when {
            typeEquals(json, "python:BoxedName") -> visitBoxedName(json)
            typeEquals(json, "python:QualifiedIdentifier") -> visitQualifiedIdentifier(json)
            else -> CompletableNode("", -1, -1)
        }
    }

    private fun visitQualifiedIdentifier(json: JsonObject): CompletableNode {
        val identifiers = json["identifiers"].asJsonArray
        return visitBoxedName(identifiers.last().asJsonObject)
    }

    private fun visitBoxedName(json: JsonObject): CompletableNode {
        val boxedValue = json["boxed_value"].asJsonObject
        return CompletableNode(visitIdentifier(boxedValue), getOffset(boxedValue), getLength(boxedValue))
    }
}
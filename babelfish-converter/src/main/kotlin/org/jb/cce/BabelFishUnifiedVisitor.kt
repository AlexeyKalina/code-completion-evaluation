package org.jb.cce

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.AssignmentNode
import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.declarations.*
import org.jb.cce.uast.statements.declarations.blocks.BlockNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.ExpressionNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import java.util.logging.Logger

open class BabelFishUnifiedVisitor {

    private val LOG = Logger.getLogger(this.javaClass.name)!!

    fun getUast(json: JsonObject): FileNode {
        return visitFileNode(json)
    }

    protected open fun visitFileNode(json: JsonObject):FileNode {
        return FileNode(-1, -1)
    }

    protected fun visitChildren(json: JsonElement, parentNode: UnifiedAstNode) {
        val values = when {
            json.isJsonObject -> json.asJsonObject.entrySet().map { it.value }
            json.isJsonArray -> json.asJsonArray
            else -> listOf()
        }
        for (value in values) {
            if (value.isJsonObject) {
                visitChild(value.asJsonObject, parentNode)
            }
            if (value.isJsonArray) {
                for (element in value.asJsonArray) {
                    if (element.isJsonObject) {
                        visitChild(element.asJsonObject, parentNode)
                    }
                }
            }
        }
    }

    protected open fun visitChild(json: JsonObject, parentNode: UnifiedAstNode) {
        when {
            typeEquals(json, "uast:FunctionGroup") -> visitFunctionDeclaration(json, parentNode)
            typeEquals(json, "uast:Function") && parentNode is MethodDeclarationNode -> visitFunctionBody(json, parentNode)
            typeEquals(json, "uast:Alias") && parentNode is MethodDeclarationNode -> visitFunctionHeader(json, parentNode)
            else -> visitChildren(json, parentNode)
        }
    }

    protected open fun visitTypeDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected open fun visitFunctionDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val function = MethodDeclarationNode(getOffset(json), getLength(json))
        visitChildren(json, function)
        addToParent(function, parentNode)
    }

    protected open fun visitFunctionHeader(json: JsonObject, parentNode: MethodDeclarationNode) {
        val nameObj = json["Name"].asJsonObject
        val header = MethodHeaderNode(visitIdentifier(nameObj), getOffset(nameObj), getLength(nameObj))
        parentNode.setHeader(header)
        visitChildren(json, parentNode)
    }

    protected open fun visitFunctionBody(json: JsonObject, parentNode: MethodDeclarationNode) {
        if (!json["Body"].isJsonObject) return
        val bodyNode = json["Body"].asJsonObject
        val body = MethodBodyNode(getOffset(bodyNode), getLength(bodyNode))
        visitChildren(bodyNode, body)
        parentNode.setBody(body)
    }

    protected open fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected open fun visitVariableAccess(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected open fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected open fun visitVariableDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected fun addToParent(node: UnifiedAstNode, parentNode: UnifiedAstNode) {
        when (parentNode) {
            is FileNode -> {
                when (node) {
                    is DeclarationNode -> parentNode.addDeclaration(node)
                    is StatementNode -> parentNode.addStatement(node)
                }
            }
            is ClassDeclarationNode -> parentNode.addMember(node as DeclarationNode)
            is VariableDeclarationNode -> parentNode.setInitExpression(node as ExpressionNode)
            is BlockNode -> parentNode.addStatement(node as StatementNode)
            is AssignmentNode -> parentNode.setAssigned(node as ExpressionNode)
            is MethodCallNode -> {
                if (node !is ExpressionNode) {
                    LOG.warning("$node is not Expression inside MethodCall")
                    return
                }
                if (parentNode.getOffset() > node.getOffset()) parentNode.prefix = node as NamedNode
                else parentNode.addArgument(node)
            }
            else -> throw UnifiedAstException("Unexpected parent $parentNode for node $node")
        }
    }

    protected open fun <T: NamedNode> visitNamedNodes (json: JsonObject, factory : (String, Int, Int) -> T): List<NamedNode> {
        val nodes = mutableListOf<T>()
        when {
            typeEquals(json, "uast:QualifiedIdentifier") -> return visitQualifiedIdentifier(json, factory)
            typeEquals(json, "uast:Identifier") -> nodes.add(factory(visitIdentifier(json), getOffset(json), getLength(json)))
            json.has("name") -> {
                val nameObj = json["name"].asJsonObject
                nodes += visitNamedNodes(nameObj, factory) as MutableList<T>
            }
        }
        return nodes
    }

    private fun <T: NamedNode> visitQualifiedIdentifier(json: JsonObject, factory : (String, Int, Int) -> T): List<NamedNode> {
        val nodes = mutableListOf<NamedNode>()
        val names = json["Names"].asJsonArray
        for (name in names) {
            nodes.add(factory(visitIdentifier(name.asJsonObject), getOffset(name.asJsonObject), getLength(name.asJsonObject)))
        }
        return nodes
    }

    protected fun visitIdentifier(json: JsonObject): String {
        if (!json.has("Name") && !json["Name"].isJsonPrimitive) {
            LOG.warning("Empty Identifier")
            return ""
        }
        return json["Name"].asString
    }

    protected fun typeEquals(json: JsonObject, type: String): Boolean {
        if (!json.has("@type") || !json["@type"].isJsonPrimitive) return false
        return json["@type"].asString == type
    }

    protected fun roleExists(json: JsonObject, role: String): Boolean {
        if (!json.has("@role") || !json["@role"].isJsonArray) return false
        return json["@role"].asJsonArray.any { el -> el.asJsonPrimitive == JsonPrimitive(role) }
    }

    protected fun getOffset(json: JsonObject): Int {
        if (!json.has("@pos") || !json["@pos"].asJsonObject.has("start")) {
            LOG.warning("Empty Offset")
            return -1
        }
        return json["@pos"].asJsonObject["start"].asJsonObject["offset"].asInt
    }

    protected fun getLength(json: JsonObject): Int {
        if (!json.has("@pos") || !json["@pos"].asJsonObject.has("end")) {
            LOG.warning("Empty Offset")
            return -1
        }
        return json["@pos"].asJsonObject["end"].asJsonObject["offset"].asInt -
                json["@pos"].asJsonObject["start"].asJsonObject["offset"].asInt
    }
}
package org.jb.cce

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.jb.cce.uast.CompletableNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodHeaderNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.references.VariableAccessNode
import java.util.logging.Logger

open class BabelFishUnifiedVisitor {

    protected val LOG = Logger.getLogger(this.javaClass.name)!!

    fun getUast(json: JsonObject): FileNode {
        return visitFileNode(json)
    }

    protected open fun visitFileNode(json: JsonObject):FileNode {
        return FileNode(-1, -1)
    }

    protected fun visitChildren(json: JsonObject, parentNode: UnifiedAstNode) {
        for ((_, value) in json.entrySet()) {
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
        when (parentNode) {
            is ClassDeclarationNode -> parentNode.addMember(function)
            is FileNode -> parentNode.addDeclaration(function)
        }
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

    protected open fun visitVariableDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected open fun <T: CompletableNode> visitCompletableNodes (json: JsonObject, factory : (String, Int, Int) -> T): List<CompletableNode> {
        val nodes = mutableListOf<T>()
        when {
            typeEquals(json, "uast:QualifiedIdentifier") -> return visitQualifiedIdentifier(json, factory)
            typeEquals(json, "uast:Identifier") -> nodes.add(factory(visitIdentifier(json), getOffset(json), getLength(json)))
            json.has("name") -> {
                val nameObj = json["name"].asJsonObject
                nodes.add(factory(visitIdentifier(nameObj), getOffset(nameObj), getLength(nameObj)))
            }
        }
        return nodes
    }

    protected fun <T: CompletableNode> visitQualifiedIdentifier(json: JsonObject, factory : (String, Int, Int) -> T): List<CompletableNode> {
        val nodes = mutableListOf<CompletableNode>()
        val names = json["Names"].asJsonArray
        for (i in 0..names.size()-2) {
            nodes.add(VariableAccessNode(visitIdentifier(names[i].asJsonObject), getOffset(names[i].asJsonObject), getLength(names[i].asJsonObject)))
        }
        val lastName = names.last().asJsonObject
        nodes.add(factory(visitIdentifier(lastName), getOffset(lastName), getLength(lastName)))
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
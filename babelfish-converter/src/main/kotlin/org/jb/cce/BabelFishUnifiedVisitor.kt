package org.jb.cce

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode

open class BabelFishUnifiedVisitor {

    fun getUast(json: JsonObject): UnifiedAstNode {
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
            typeEquals(json, "uast:Function") -> visitFunctionBody(json, parentNode as MethodDeclarationNode)
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

    protected open fun visitFunctionBody(json: JsonObject, parentNode: MethodDeclarationNode) {
        val body = MethodBodyNode(getOffset(json), getLength(json))
        visitChildren(json, body)
        parentNode.setBody(body)
    }

    protected open fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected open fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected open fun visitVariableDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
    }

    protected fun visitIdentifier(json: JsonObject): String {
        if (!json.has("Name") && !json["Name"].isJsonPrimitive) return ""
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
        if (!json.has("@pos") || !json["@pos"].asJsonObject.has("start")) return -1
        return json["@pos"].asJsonObject["start"].asJsonObject["offset"].asInt
    }

    protected fun getLength(json: JsonObject): Int {
        if (!json.has("@pos") || !json["@pos"].asJsonObject.has("end")) return -1
        return json["@pos"].asJsonObject["end"].asJsonObject["offset"].asInt -
                json["@pos"].asJsonObject["start"].asJsonObject["offset"].asInt
    }
}
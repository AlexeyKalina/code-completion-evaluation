package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.util.ConverterHelper.addToParent
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodHeaderNode
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode

class BabelFishJavaVisitor: BabelFishUnifiedVisitor() {

    override fun visitFileNode(json: JsonObject): FileNode {
        val file = FileNode(getOffset(json), getLength(json))
        visitChildren(json, file)
        return file
    }

    override fun visitChild(json: JsonObject, parentNode: UnifiedAstNode) {
        when {
            typeEquals(json, "java:TypeDeclaration") -> visitTypeDeclaration(json, parentNode)
            typeEquals(json, "java:MethodDeclaration") ||
                    typeEquals(json, "java:Initializer") -> visitMethodDeclaration(json, parentNode)
            typeEquals(json, "java:MethodInvocation") ||
                typeEquals(json, "java:ExpressionMethodReference") -> visitMethodCall(json, parentNode)
            //TODO: support lambda
            typeEquals(json, "java:LambdaExpression") -> return
            typeEquals(json, "java:ClassInstanceCreation") -> visitInstanceCreation(json, parentNode)
            typeEquals(json, "java:VariableDeclarationFragment") ||
                typeEquals(json, "java:EnumConstantDeclaration") -> visitVariableDeclaration(json, parentNode)
            (roleExists(json, "Expression") || roleExists(json, "Argument") || roleExists(json, "Assignment"))
                && (typeEquals(json, "uast:Identifier") ||
                    typeEquals(json, "uast:QualifiedIdentifier")) -> visitVariableAccess(json, parentNode)
            typeEquals(json, "java:FieldAccess") -> visitFieldAccess(json, parentNode)
            else -> super.visitChild(json, parentNode)
        }
    }

    private fun visitMethodDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val methodDeclaration = MethodDeclarationNode(getOffset(json), getLength(json))
        if (json.has("name") && json["name"].isJsonObject) {
            val name = json["name"].asJsonObject
            methodDeclaration.setHeader(MethodHeaderNode(visitIdentifier(name), getOffset(name), getLength(name)))
        }
        if (json.has("body") && json["body"].isJsonObject) {
            val bodyJson = json["body"].asJsonObject
            val body = MethodBodyNode(getOffset(bodyJson), getLength(bodyJson))
            methodDeclaration.setBody(body)
            visitChildren(bodyJson, body)
        }
        addToParent(methodDeclaration, parentNode)
    }

    private fun visitInstanceCreation(json: JsonObject, parentNode: UnifiedAstNode) {
        val typeObj = json["type"].asJsonObject
        if (typeEquals(typeObj, "java:SimpleType")) {
            visitMethodCall(typeObj, parentNode)
        }
    }

    override fun visitVariableDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableDeclaration = VariableDeclarationNode(visitIdentifier(json["name"].asJsonObject), getOffset(json), getLength(json))
        visitChildren(json, variableDeclaration)
        addToParent(variableDeclaration, parentNode)
    }

    override fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val fieldAccessNodes = visitNamedNodes(json) { name, offset, length -> FieldAccessNode(name, offset, length) }
        for (node in fieldAccessNodes) {
            addToParent(node, parentNode)
        }
    }

    override fun visitVariableAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableAccessNodes = visitNamedNodes(json) { name, offset, length -> VariableAccessNode(name, offset, length) }
        for (node in variableAccessNodes) {
            addToParent(node, parentNode)
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val methodCallNodes = visitNamedNodes(json) { name, offset, length -> MethodCallNode(name, offset, length) }
        for (node in methodCallNodes) {
            addToParent(node, parentNode)
        }
        val lastNode = methodCallNodes.last()

        visitChildren(json, lastNode)
    }

    override fun visitTypeDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val name = if (json.has("name")) visitIdentifier(json["name"].asJsonObject) else ""
        val classDeclaration = ClassDeclarationNode(name, getOffset(json), getLength(json))

        visitChildren(json, classDeclaration)

        addToParent(classDeclaration, parentNode)
    }
}
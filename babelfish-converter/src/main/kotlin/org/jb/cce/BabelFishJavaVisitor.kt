package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodDeclarationNode
import org.jb.cce.uast.statements.declarations.MethodHeaderNode
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.LambdaExpressionNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.ArrayAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode

class BabelFishJavaVisitor(path: String, text: String): BabelFishUnifiedVisitor(path, text)  {

    override fun visitFileNode(json: JsonObject): FileNode {
        val file = FileNode(getOffset(json), getLength(json), path, text)
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
            typeEquals(json, "java:LambdaExpression") -> visitLambdaExpression(json, parentNode)
            typeEquals(json, "java:ArrayInitializer") -> visitArrayInitializer(json, parentNode)
            typeEquals(json, "java:ClassInstanceCreation") -> visitInstanceCreation(json, parentNode)
            typeEquals(json, "java:ArrayAccess") -> visitArrayAccess(json, parentNode)
            typeEquals(json, "java:VariableDeclarationFragment") ||
                typeEquals(json, "java:SingleVariableDeclaration") ||
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
            val instanceCreationCall = visitCall(typeObj, parentNode)
            visitChildren(json["arguments"], instanceCreationCall)
        }
    }

    private fun visitArrayInitializer(json: JsonObject, parentNode: UnifiedAstNode) {
        if (!json.has("expressions")) return
        for (expression in json["expressions"].asJsonArray) {
            if (!expression.isJsonObject) continue
            if (typeEquals(expression.asJsonObject, "uast:Identifier") || typeEquals(expression.asJsonObject, "uast:QualifiedIdentifier"))
                visitVariableAccess(expression.asJsonObject, parentNode)
            else visitChild(expression.asJsonObject, parentNode)
        }
    }

    override fun visitVariableDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableDeclaration = VariableDeclarationNode(visitIdentifier(json["name"].asJsonObject), getOffset(json), getLength(json))
        visitChildren(json, variableDeclaration)
        addToParent(variableDeclaration, parentNode)
    }

    override fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val fieldAccessNodes = visitNamedNodes(json) { name, offset, length, _ -> FieldAccessNode(name, offset, length) }
        for (node in fieldAccessNodes) {
            addToParent(node, parentNode)
        }
    }

    override fun visitVariableAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableAccessNodes = visitNamedNodes(json) { name, offset, length, first ->
            if (first) VariableAccessNode(name, offset, length) as NamedNode
            else FieldAccessNode(name, offset, length)
        }
        for (node in variableAccessNodes) {
            addToParent(node, parentNode)
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val methodCallNode = visitCall(json, parentNode)
        visitChildren(json, methodCallNode)
    }

    override fun visitTypeDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val name = if (json.has("name")) visitIdentifier(json["name"].asJsonObject) else ""
        val classDeclaration = ClassDeclarationNode(name, getOffset(json), getLength(json))

        visitChildren(json, classDeclaration)

        addToParent(classDeclaration, parentNode)
    }

    override fun visitArrayAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val arrayAccessNodes = visitNamedNodes(json["array"].asJsonObject) { name, offset, length, first -> ArrayAccessNode(name, offset, length) }
        for (node in arrayAccessNodes)
            addToParent(node, parentNode)
        val index = json["index"].asJsonObject
        if (typeEquals(index, "uast:Identifier") || typeEquals(index, "uast:QualifiedIdentifier")) {
            val indexNodes = visitNamedNodes(index) { name, offset, length, first ->
                if (first) VariableAccessNode(name, offset, length) as NamedNode
                else FieldAccessNode(name, offset, length)
            }
            for (node in indexNodes)
                addToParent(node, arrayAccessNodes.last())
        }
        visitChild(index, arrayAccessNodes.last())
    }

    private fun visitLambdaExpression(json: JsonObject, parentNode: UnifiedAstNode) {
        val lambda = LambdaExpressionNode(getOffset(json), getLength(json))
        val body = json["body"].asJsonObject
        when {
            typeEquals(body, "uast:Block") -> {
                val block = MethodBodyNode(getOffset(body), getLength(body))
                lambda.setBody(block)
                visitChildren(body, block)
            }
            else -> visitChild(body, lambda)
        }
        addToParent(lambda, parentNode)
    }

    private fun visitCall(json: JsonObject, parentNode: UnifiedAstNode): MethodCallNode {
        val methodCallNodes = visitNamedNodes(json) { name, offset, length, _ -> MethodCallNode(name, offset, length) }
        for (node in methodCallNodes) {
            addToParent(node, parentNode)
        }
        return methodCallNodes.last() as MethodCallNode
    }
}
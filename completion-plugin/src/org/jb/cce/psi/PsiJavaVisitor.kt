package org.jb.cce.psi

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.java.PsiArrayAccessExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.*
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.NodeProperties
import org.jb.cce.uast.statements.declarations.*
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.AnonymousClassNode
import org.jb.cce.uast.statements.expressions.LambdaExpressionNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.ArrayAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.references.TypeReferenceNode
import java.util.*

class PsiJavaVisitor(private val path: String, private val text: String) : PsiVisitor, JavaRecursiveElementVisitor() {
    private var _file: FileNode? = null

    override fun getFile(): FileNode = _file ?: throw PsiConverterException("Invoke 'accept' with visitor on Bash PSI first")

    private val stackOfNodes: Deque<UnifiedAstNode> = ArrayDeque<UnifiedAstNode>()

    private var isArgument = false

    override fun visitJavaFile(file: PsiJavaFile) {
        _file = FileNode(file.textOffset, file.textLength, path, text)
        stackOfNodes.addLast(_file)
        super.visitJavaFile(file)
    }

    override fun visitClass(node: PsiClass) {
        val name = node.nameIdentifier
        val header = if (name != null) ClassHeaderNode(name.text, name.textOffset, name.textLength)
        else ClassHeaderNode("<no_name>", node.textOffset, 0)
        val classNode = ClassDeclarationNode(header, node.textOffset, node.textLength)
        addToParent(classNode)
        stackOfNodes.addLast(classNode)
        var bodyNode: PsiElement? = node.lBrace
        while (bodyNode != null) {
            bodyNode.accept(this)
            bodyNode = bodyNode.nextSibling
        }
        stackOfNodes.removeLast()
    }

    override fun visitMethod(node: PsiMethod) {
        val bodyNode = node.body ?: return
        val function = MethodDeclarationNode(node.textOffset, node.textLength)
        val name = node.nameIdentifier
        val header = MethodHeaderNode(name?.text ?: "<empty_name>",
                name?.textOffset ?: throw PsiConverterException("Empty name"), name.textLength)
        val body = MethodBodyNode(bodyNode.textOffset, bodyNode.textLength)
        function.setHeader(header)
        function.setBody(body)
        addToParent(function)
        stackOfNodes.addLast(body)
        super.visitCodeBlock(node.body)
        stackOfNodes.removeLast()
    }

    override fun visitClassInitializer(node: PsiClassInitializer) {
        val function = MethodDeclarationNode(node.textOffset, node.textLength)
        val body = MethodBodyNode(node.body.textOffset, node.body.textLength)
        function.setBody(body)
        addToParent(function)
        stackOfNodes.addLast(body)
        super.visitClassInitializer(node)
        stackOfNodes.removeLast()
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        val name = expression.methodExpression.referenceName
        val methodCall = MethodCallNode(name ?: throw PsiConverterException("Empty method name"),
                expression.methodExpression.textOffset, name.length, getMethodCallProperties(expression))
        addToParent(methodCall)
        stackOfNodes.addLast(methodCall)
        val qualifier = expression.methodExpression.qualifier
        visitPotentialExpression(qualifier)
        visitArgumentList(expression.argumentList)
        stackOfNodes.removeLast()
    }

    override fun visitCallExpression(callExpression: PsiCallExpression) {
        if (callExpression is PsiNewExpressionImpl) {
            when {
                callExpression.arrayInitializer != null -> visitArrayInitializerExpression(callExpression.arrayInitializer)
                callExpression.anonymousClass != null -> visitAnonymousClass(callExpression)
                callExpression.classReference != null -> visitConstructorInvocation(callExpression)
            }
        } else super.visitCallExpression(callExpression)
    }

    private fun visitAnonymousClass(callExpression: PsiNewExpressionImpl) {
        val classNode = callExpression.anonymousClass ?: return
        val anonymousClass = AnonymousClassNode(classNode.textOffset, classNode.textLength)
        addToParent(anonymousClass)
        stackOfNodes.addLast(anonymousClass)
        visitConstructorInvocation(callExpression)
        val prevValue = isArgument
        isArgument = false
        visitClass(classNode)
        isArgument = prevValue
        stackOfNodes.removeLast()
    }

    private fun visitConstructorInvocation(callExpression: PsiNewExpressionImpl) {
        val typeName = callExpression.classOrAnonymousClassReference ?: return
        val name = typeName.referenceName ?: typeName.text
        val methodCall = MethodCallNode(name, typeName.textOffset, name.length, getMethodCallProperties(callExpression))
        addToParent(methodCall)
        stackOfNodes.addLast(methodCall)
        visitArgumentList(callExpression.argumentList)
        stackOfNodes.removeLast()
    }

    private fun getMethodCallProperties(expression: PsiCallExpression): NodeProperties {
        val resolvedMethod = expression.resolveMethod()
        val isStatic = resolvedMethod?.modifierList?.text?.contains("static") ?: false
        return getNodeProperties(TypeProperty.METHOD_CALL, isStatic, getPackageName(resolvedMethod))
    }

    private fun getPackageName(resolvedReference: PsiElement?) = (resolvedReference?.containingFile as? PsiJavaFile)?.packageName ?: "<unknown package>"

    private fun visitArgumentList(argumentList: PsiExpressionList?) {
        val prevValue = isArgument
        isArgument = true
        if (argumentList != null) super.visitElement(argumentList)
        isArgument = prevValue
    }

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {
        val lambda = LambdaExpressionNode(expression.textOffset, expression.textLength)
        addToParent(lambda)
        val body = expression.body
        val prevValue = isArgument
        isArgument = false
        if (body is PsiExpression) {
            stackOfNodes.addLast(lambda)
            visitPotentialExpression(body)
            stackOfNodes.removeLast()
        } else if (body is PsiCodeBlock) {
            val block = MethodBodyNode(body.textOffset, body.textLength)
            lambda.setBody(block)
            stackOfNodes.addLast(block)
            visitCodeBlock(body)
            stackOfNodes.removeLast()
        }
        isArgument = prevValue
    }

    override fun visitArrayAccessExpression(expression: PsiArrayAccessExpression) {
        if (expression is PsiArrayAccessExpressionImpl) {
            val bracket = expression.findChildByRole(ChildRole.LBRACKET)
            if (bracket != null) {
                val arrayAccess = ArrayAccessNode(expression.textOffset, expression.textLength, bracket.startOffset)
                addToParent(arrayAccess)
                stackOfNodes.addLast(arrayAccess)
                visitPotentialExpression(expression.arrayExpression)
                visitPotentialExpression(expression.indexExpression)
                stackOfNodes.removeLast()
            }
        } else super.visitArrayAccessExpression(expression)
    }

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        val name = reference.referenceName ?: reference.text
        val resolvedRef = reference.resolve()
        val packageName = getPackageName(resolvedRef)
        when (resolvedRef) {
            is PsiField -> {
                val isStatic = resolvedRef.modifierList?.text?.contains("static") ?: false
                val field = FieldAccessNode(name, reference.textOffset, name.length, getNodeProperties(TypeProperty.FIELD, isStatic, packageName))
                addToParent(field)
                stackOfNodes.addLast(field)
                super.visitReferenceElement(reference)
                stackOfNodes.removeLast()
            }
            is PsiLocalVariable -> {
                resolvedRef.containingFile
                val node = VariableAccessNode(name, reference.textOffset, name.length, getNodeProperties(TypeProperty.VARIABLE, false, packageName))
                addToParent(node)
            }
            is PsiParameter -> {
                val node = VariableAccessNode(name, reference.textOffset, name.length, getNodeProperties(TypeProperty.VARIABLE, false, packageName))
                addToParent(node)
            }
            is PsiClass -> {
                val node = TypeReferenceNode(name, reference.textOffset, name.length)
                addToParent(node)
            }
        }
    }

    override fun visitVariable(variable: PsiVariable) {
        val name = variable.nameIdentifier
        val variableDeclaration = VariableDeclarationNode(name?.text ?: throw PsiConverterException("Empty variable name"),
                name.textOffset, name.textLength)
        addToParent(variableDeclaration)
        stackOfNodes.addLast(variableDeclaration)
        if (variable !is PsiEnumConstant) super.visitVariable(variable)
        stackOfNodes.removeLast()
    }

    override fun visitPackageStatement(statement: PsiPackageStatement?) {
    }

    override fun visitImportStatement(statement: PsiImportStatement?) {
    }

    override fun visitImportStaticStatement(statement: PsiImportStaticStatement?) {
    }

    override fun visitAnnotation(annotation: PsiAnnotation?) {
    }

    override fun visitTypeElement(type: PsiTypeElement?) {
    }

    override fun visitReferenceList(list: PsiReferenceList?) {
    }

    override fun visitComment(comment: PsiComment?) {
    }

    override fun visitResourceExpression(expression: PsiResourceExpression?) {
    }

    override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression?) {
    }

    private fun visitPotentialExpression(element: PsiElement?) {
        element?.accept(this)
    }

    private fun getNodeProperties(tokenType: TypeProperty, isStatic: Boolean, packageName: String): NodeProperties {
        val properties = NodeProperties()
        properties.tokenType = tokenType
        properties.isArgument = isArgument
        properties.isStatic = isStatic
        properties.packageName = packageName
        return properties
    }

    private fun addToParent(node: UnifiedAstNode) {
        val parentNode = stackOfNodes.last
        if (parentNode is CompositeNode) parentNode.addChild(node)
        else throw UnifiedAstException("Unexpected parent $parentNode for node $node")
    }
}
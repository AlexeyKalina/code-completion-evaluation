package org.jb.cce

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.uast.TokenNode
import org.jb.cce.util.text
import org.jb.cce.visitors.EvaluationRootVisitor

open class TextTokensBuilder : UastBuilder() {
    override fun build(file: VirtualFile, rootVisitor: EvaluationRootVisitor): TextFragmentNode {
        val text = file.text()
        val textFragment = TextFragmentNode(0, text.length, file.path, text)
        var curToken = ""
        var tokenOffset = 0
        var offset = 0
        var insideQuote = false
        for (ch in text) {
            when {
                ch == '"' || ch == '\'' -> insideQuote = !insideQuote
                insideQuote -> {}
                isIdentifierPart(ch) -> {
                    if (curToken.isEmpty()) tokenOffset = offset
                    curToken += ch
                }
                isIdentifier(curToken) -> {
                    textFragment.addChild(TokenNode(curToken, tokenOffset, curToken.length))
                    curToken = ""
                }
                else -> curToken = ""
            }
            offset++
        }
        return findRoot(textFragment, rootVisitor)
    }

    open fun isIdentifierPart(ch: Char) = ch.isLetter()
    open fun isIdentifier(token: String) = token.isNotEmpty()
}

class JavaTokensBuilder : TextTokensBuilder() {
    override fun isIdentifierPart(ch: Char) = StringUtil.isJavaIdentifierPart(ch)
    override fun isIdentifier(token: String) = StringUtil.isJavaIdentifier(token)
}
package org.jb.cce.evaluation

import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.uast.TokenNode
import org.jb.cce.util.text
import org.jb.cce.visitors.EvaluationRootVisitor

open class TextTokensBuilder(private val tokenFilter: TokenFilter) : UastBuilder() {
    override fun build(file: VirtualFile, rootVisitor: EvaluationRootVisitor): TextFragmentNode {
        val text = file.text()
        val textFragment = TextFragmentNode(0, text.length, file.path, text)
        var offset = 0
        for (line in text.lines()) {
            var curToken = ""
            var tokenOffset = 0
            if (tokenFilter.isComment(line)) {
                offset += line.length + 1
                continue
            }
            for (ch in line) {
                if (ch == '_' || ch.isLetter() || (ch.isDigit() && curToken.isNotEmpty())) {
                    if (curToken.isEmpty()) tokenOffset = offset
                    curToken += ch
                } else if (tokenFilter.test(curToken)) {
                    textFragment.addChild(TokenNode(curToken, tokenOffset, curToken.length))
                    curToken = ""
                }
                offset++
            }
            if (tokenFilter.test(curToken)) textFragment.addChild(TokenNode(curToken, tokenOffset, curToken.length))
            offset++
        }
        return findRoot(textFragment, rootVisitor)
    }
}
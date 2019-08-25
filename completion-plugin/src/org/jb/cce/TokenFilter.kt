package org.jb.cce

import com.intellij.openapi.util.text.StringUtil

open class TokenFilter {
    open fun isComment(line: String) = line.trim().startsWith("//")
    open fun test(token: String) = token.isNotEmpty()
}

class JavaTokenFilter : TokenFilter() {
    override fun test(token: String) = StringUtil.isJavaIdentifier(token)
}

class BashTokenFilter : TokenFilter() {
    override fun isComment(line: String) = line.trim().startsWith("#")
}

class PythonTokenFilter : TokenFilter() {
    override fun isComment(line: String) = line.trim().startsWith("#")
}
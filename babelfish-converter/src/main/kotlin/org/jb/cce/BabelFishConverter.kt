package org.jb.cce

import com.google.gson.JsonParser
import org.jb.cce.exceptions.BabelFishClientException
import org.jb.cce.uast.FileNode

class BabelFishConverter {
    fun convert(babelFishAst: String, language: Language): FileNode {
        val json = JsonParser().parse(babelFishAst).asJsonObject!!
        return when (language) {
            Language.JAVA -> BabelFishJavaVisitor().getUast(json)
            Language.CSHARP -> BabelFishCSharpVisitor().getUast(json)
            Language.PYTHON -> BabelFishPythonVisitor().getUast(json)
            Language.UNSUPPORTED -> throw BabelFishClientException("Unsupported language")
        }
    }
}
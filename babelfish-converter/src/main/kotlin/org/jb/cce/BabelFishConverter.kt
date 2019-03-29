package org.jb.cce

import com.google.gson.JsonParser
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode

class BabelFishConverter {
    fun convert(babelFishAst: String, language: Language): FileNode {
        val json = JsonParser().parse(babelFishAst).asJsonObject!!
        return when (language) {
            Language.JAVA -> BabelFishJavaVisitor().getUast(json)
            Language.PYTHON -> BabelFishPythonVisitor().getUast(json)
            Language.ANOTHER -> BabelFishUnifiedVisitor().getUast(json)
        }
    }
}
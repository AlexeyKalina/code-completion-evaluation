package org.jb.cce.evaluation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.exceptions.NullRootException
import org.jb.cce.psi.PsiConverter
import org.jb.cce.uast.Language
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.visitors.EvaluationRootVisitor

abstract class UastBuilder {
    companion object {
        fun create(project: Project, languageName: String, allTokens: Boolean): UastBuilder {
            val language = Language.resolve(languageName)
            return if (allTokens)
                when (language) {
                    Language.JAVA -> TextTokensBuilder(JavaTokenFilter())
                    Language.PYTHON -> TextTokensBuilder(PythonTokenFilter())
                    Language.BASH -> TextTokensBuilder(BashTokenFilter())
                    else -> TextTokensBuilder(TokenFilter())
                }
            else PsiConverter(project, language)
        }
    }

    protected fun findRoot(uast: TextFragmentNode, rootVisitor: EvaluationRootVisitor): TextFragmentNode {
        uast.accept(rootVisitor)
        return rootVisitor.getRoot() ?: throw NullRootException(uast.path)
    }

    abstract fun build(file: VirtualFile, rootVisitor: EvaluationRootVisitor): TextFragmentNode
}
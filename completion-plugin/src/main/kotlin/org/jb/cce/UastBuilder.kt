package org.jb.cce

import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.io.input.UnixLineEndingInputStream
import org.jb.cce.psi.PsiConverter
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.Language
import org.jb.cce.uast.TextFragmentNode

abstract class UastBuilder {
    companion object {
        fun create(project: Project, languageName: String, allTokens: Boolean): UastBuilder {
            val language = Language.resolve(languageName)
            return if (allTokens)
                when (language) {
                    Language.JAVA -> JavaTokensBuilder()
                    else -> TextTokensBuilder()
                }
            else when (language) {
                Language.PYTHON -> PsiConverter(project, language)
                Language.BASH -> PsiConverter(project, language)
                Language.JAVA -> BabelFishBuilderWrapper(language) { path, text -> BabelFishJavaVisitor(path, text) }
                Language.CSHARP -> BabelFishBuilderWrapper(language) { path, text -> BabelFishCSharpVisitor(path, text) }
                Language.ANOTHER -> throw java.lang.UnsupportedOperationException("Use All tokens statement type with this language.")
                Language.UNSUPPORTED -> throw UnsupportedOperationException("Unsupported language.")
            }
        }
    }

    abstract fun build(file: VirtualFile): TextFragmentNode

    private class BabelFishBuilderWrapper(language: Language, private val visitorFactory: (path: String, text: String) -> BabelFishUnifiedVisitor) : UastBuilder() {
        private val client = BabelFishClient(language)

        override fun build(file: VirtualFile): FileNode {
            val babelFishUast = client.parse(file.text())
            val json = JsonParser().parse(babelFishUast).asJsonObject!!
            return visitorFactory(file.path, file.text()).getUast(json)
        }
    }

    protected fun VirtualFile.text(): String {
        return UnixLineEndingInputStream(this.inputStream, false).bufferedReader().use { it.readText() }
    }
}
package org.jb.cce.psi

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jb.cce.UastBuilder
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.Language
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.util.FilesHelper
import org.jb.cce.util.text
import org.jb.cce.visitors.EvaluationRootVisitor

class PsiConverter(private val project: Project, val language: Language) : UastBuilder() {
    private val dumbService = DumbService.getInstance(project)

    override fun build(file: VirtualFile, rootVisitor: EvaluationRootVisitor): TextFragmentNode {
        val psi = dumbService.runReadActionInSmartMode<PsiFile> {
            PsiManager.getInstance(project).findFile(file)
        } ?: throw PsiConverterException("Cannot get PSI of file ${file.path}")

        val filePath = FilesHelper.getRelativeToProjectPath(project, file.path)

        val uast = when (language) {
            Language.BASH -> getUast(PsiBashVisitor(filePath, file.text()), psi)
            Language.JAVA -> getUast(PsiJavaVisitor(filePath, file.text()), psi)
            Language.PYTHON -> getUast(PsiPythonVisitor(filePath, file.text()), psi)
            Language.ANOTHER -> throw PsiConverterException("Use All tokens statement type with this language.")
            Language.UNSUPPORTED -> throw PsiConverterException("Unsupported language.")
        }
        return findRoot(uast, rootVisitor)
    }

    private fun<T> getUast(visitor: T, psi: PsiElement): FileNode where T: PsiElementVisitor, T : PsiVisitor {
        dumbService.runReadActionInSmartMode {
            assert(!dumbService.isDumb) { "Generating actions during indexing." }
            psi.accept(visitor)
        }
        return visitor.getFile()
    }
}
package org.jb.cce.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jb.cce.UastBuilder
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.Language
import org.jb.cce.uast.FileNode

class PsiConverter(val project: Project, val language: Language) : UastBuilder() {
    override fun build(file: VirtualFile): FileNode {
        val psi = ApplicationManager.getApplication().runReadAction<PsiFile> {
            PsiManager.getInstance(project).findFile(file)
        } ?: throw PsiConverterException("Cannot get PSI of file")

        return when (language) {
            Language.PYTHON -> {
                val pythonVisitor = PsiPythonVisitor(file.path, file.text())
                psi.accept(pythonVisitor)
                pythonVisitor.file
            }
            else -> throw PsiConverterException("Unsupported language")
        }
    }
}
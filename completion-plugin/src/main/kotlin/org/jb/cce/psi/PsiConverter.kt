package org.jb.cce.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jb.cce.UastBuilder
import org.jb.cce.psi.exceptions.PsiConverterException
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.Language

class PsiConverter(private val project: Project, val language: Language) : UastBuilder() {
    override fun build(file: VirtualFile): FileNode {
        val psi = ApplicationManager.getApplication().runReadAction<PsiFile> {
            PsiManager.getInstance(project).findFile(file)
        } ?: throw PsiConverterException("Cannot get PSI of file ${file.path}")

        return when (language) {
            Language.BASH -> {
                val bashVisitor = PsiBashVisitor(file.path, file.text())
                ApplicationManager.getApplication().runReadAction {
                    psi.accept(bashVisitor)
                }
                bashVisitor.file
            }
            Language.PYTHON -> {
                val pythonVisitor = PsiPythonVisitor(file.path, file.text())
                ApplicationManager.getApplication().runReadAction {
                    psi.accept(pythonVisitor)
                }
                pythonVisitor.file
            }
            else -> throw PsiConverterException("Unsupported language")
        }
    }
}
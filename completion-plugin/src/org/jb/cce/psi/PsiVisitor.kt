package org.jb.cce.psi

import org.jb.cce.uast.FileNode

interface PsiVisitor {
    fun getFile(): FileNode
}
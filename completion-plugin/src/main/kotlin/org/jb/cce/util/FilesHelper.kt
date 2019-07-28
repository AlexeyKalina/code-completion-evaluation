package org.jb.cce.util

import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import org.jb.cce.uast.Language

object FilesHelper {
    fun getFiles(selectedFiles: List<VirtualFile>): Map<Language, Set<VirtualFile>> {
        val language2files = mutableMapOf<Language, MutableSet<VirtualFile>>()
        for (file in selectedFiles) {
            VfsUtilCore.iterateChildrenRecursively(file, VirtualFileFilter.ALL, object : ContentIterator {
                override fun processFile(fileOrDir: VirtualFile): Boolean {
                    val extension = fileOrDir.extension
                    if (fileOrDir.isDirectory || extension == null) return true

                    val language = Language.resolve(extension)
                    if (language != Language.UNSUPPORTED) {
                        language2files.computeIfAbsent(language) { mutableSetOf() }.add(fileOrDir)
                    }
                    return true
                }
            })
        }
        return language2files
    }
}
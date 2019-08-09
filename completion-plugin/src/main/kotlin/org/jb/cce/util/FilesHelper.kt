package org.jb.cce.util

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter

object FilesHelper {
    fun getFiles(selectedFiles: List<VirtualFile>): Map<String, Set<VirtualFile>> {
        val language2files = mutableMapOf<String, MutableSet<VirtualFile>>()
        for (file in selectedFiles) {
            VfsUtilCore.iterateChildrenRecursively(file, VirtualFileFilter.ALL, object : ContentIterator {
                override fun processFile(fileOrDir: VirtualFile): Boolean {
                    val extension = fileOrDir.extension
                    if (fileOrDir.isDirectory || extension == null) return true

                    val language = getLanguageByExtension(extension)
                    if (language != null) {
                        language2files.computeIfAbsent(language.displayName) { mutableSetOf() }.add(fileOrDir)
                    }
                    return true
                }
            })
        }
        return language2files
    }

    fun getLanguageByExtension(ext: String): Language? {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension(ext) as? LanguageFileType ?: return null
        return fileType.language
    }
}
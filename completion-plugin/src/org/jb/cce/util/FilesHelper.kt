package org.jb.cce.util

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.apache.commons.io.input.UnixLineEndingInputStream
import org.jb.cce.EvaluationWorkspace
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.nio.file.Paths

object FilesHelper {
    fun getFilesOfLanguage(project: Project, evaluationRoots: List<String>, language: String): List<VirtualFile> {
        return getFiles(project, evaluationRoots.map { getFile(project, it) })[language]?.toList()
            ?: throw IllegalArgumentException("No files for $language found")
    }

    fun getFiles(project: Project, evaluationRoots: List<VirtualFile>): Map<String, Set<VirtualFile>> {
        val language2files = mutableMapOf<String, MutableSet<VirtualFile>>()
        val index = ProjectRootManager.getInstance(project).fileIndex
        for (file in evaluationRoots) {
            VfsUtilCore.iterateChildrenRecursively(file, GlobalSearchScope.projectScope(project), object : ContentIterator {
                override fun processFile(fileOrDir: VirtualFile): Boolean {
                    val extension = fileOrDir.extension
                    if (fileOrDir.isDirectory || extension == null) return true

                    val language = getLanguageByExtension(extension)
                    if (language != null && shouldEvaluateOnFile(language, fileOrDir)) {
                        language2files.computeIfAbsent(language.displayName) { mutableSetOf() }.add(fileOrDir)
                    }
                    return true
                }

                private fun shouldEvaluateOnFile(language: Language, fileOrDir: VirtualFile): Boolean {
                    if (language.displayName == "Java") {
                        return index.isInSourceContent(fileOrDir)
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

    fun getRelativeToProjectPath(project: Project, path: String): String {
        val projectPath = project.basePath
        return if (projectPath == null) path else Paths.get(projectPath).relativize(Paths.get(path)).toString()
    }

    fun getFile(project: Project, relativePath: String): VirtualFile =
            VfsUtil.findFile(Paths.get(project.basePath ?: "").resolve(relativePath), true)
                    ?: throw FileNotFoundException("File not found: $relativePath")
}

fun VirtualFile.text(): String {
    return UnixLineEndingInputStream(this.inputStream, false).bufferedReader().use { it.readText() }
}

fun EvaluationWorkspace.pathToConfig() = path().resolve(ConfigFactory.DEFAULT_CONFIG_NAME)
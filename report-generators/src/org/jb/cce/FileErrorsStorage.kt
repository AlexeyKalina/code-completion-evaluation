package org.jb.cce

import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileErrorSerializer
import java.io.File
import java.nio.file.Paths

class FileErrorsStorage(val storageDir: String) {
    companion object {
        private val fileErrorSerializer = FileErrorSerializer()
    }

    fun saveError(error: FileErrorInfo) {
        val json = fileErrorSerializer.serialize(error)
        Paths.get(storageDir, "${Paths.get(error.path).fileName}.json").toFile().writeText(json)
    }

    fun getErrors(): List<FileErrorInfo> {
        return File(storageDir).listFiles()?.map { fileErrorSerializer.deserialize(it.readText()) } ?: emptyList()
    }
}
package org.jb.cce.storages

import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileErrorSerializer
import java.io.File
import java.nio.file.Paths

class FileErrorsStorage(storageDir: String) : EvaluationStorage(storageDir) {
    companion object {
        private val fileErrorSerializer = FileErrorSerializer()
    }

    fun saveError(error: FileErrorInfo) {
        val json = fileErrorSerializer.serialize(error)
        val path = Paths.get(storageDir, "${Paths.get(error.path).fileName}.json")
        saveFile(path.toString(), json)
    }

    fun getErrors(): List<FileErrorInfo> {
        return File(storageDir).listFiles()?.map { fileErrorSerializer.deserialize(readFile(it.path)) } ?: emptyList()
    }
}
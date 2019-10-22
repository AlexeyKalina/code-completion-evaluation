package org.jb.cce.storages

import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileErrorSerializer
import java.nio.file.Paths

class FileErrorsStorage(storageDir: String) {
    companion object {
        private val fileErrorSerializer = FileErrorSerializer()
    }
    private val keyValueStorage = FileArchivesStorage(storageDir)

    fun saveError(error: FileErrorInfo) {
        val json = fileErrorSerializer.serialize(error)
        keyValueStorage.save("${Paths.get(error.path).fileName}.json", json)
    }

    fun getErrors(): List<FileErrorInfo> {
        return keyValueStorage.getKeys().map { fileErrorSerializer.deserialize(keyValueStorage.get(it)) }
    }
}
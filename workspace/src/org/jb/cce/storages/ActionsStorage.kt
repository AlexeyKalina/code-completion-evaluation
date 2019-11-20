package org.jb.cce.storages

import org.jb.cce.actions.ActionSerializer
import org.jb.cce.actions.FileActions
import java.nio.file.Paths

class ActionsStorage(storageDir: String) {
    companion object {
        private val actionSerializer = ActionSerializer()
    }
    private val keyValueStorage = FileArchivesStorage(storageDir)
    private var filesCounter = 0

    fun saveActions(actions: FileActions) {
        filesCounter++
        keyValueStorage.save("${Paths.get(actions.path).fileName}($filesCounter).json", actionSerializer.serialize(actions))
    }

    fun computeSessionsCount(): Int {
        var count = 0
        for (file in getActionFiles())
            count += actionSerializer.getSessionsCount(keyValueStorage.get(file))
        return count
    }

    fun getActionFiles(): List<String> = keyValueStorage.getKeys().sortedBy {
        it.substringAfterLast('(').substringBefore(')').toInt()
    }

    fun getActions(path: String): FileActions {
        return actionSerializer.deserialize(keyValueStorage.get(path))
    }
}
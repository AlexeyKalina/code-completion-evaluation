package org.jb.cce

import org.jb.cce.actions.ActionSerializer
import org.jb.cce.actions.FileActions
import org.jb.cce.info.FileErrorInfo
import java.io.File
import java.nio.file.Paths

class ActionsStorage(val storageDir: String) {
    companion object {
        private val actionSerializer = ActionSerializer()
    }

    private var filesCounter = 0

    fun saveActions(actions: FileActions) {
        val actionsPath = Paths.get(storageDir, "${File(actions.path).name}($filesCounter).json")
        filesCounter++
        actionsPath.toFile().writeText(actionSerializer.serialize(actions))
    }

    fun computeSessionsCount(): Int {
        var count = 0
        for (file in getActionFiles())
            count += actionSerializer.getSessionsCount(file.readText())
        return count
    }

    fun getActionFiles(): List<File> {
        return File(storageDir).listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
    }

    fun getActions(file: File): FileActions {
        return actionSerializer.deserialize(file.readText())
    }
}
package org.jb.cce.storages

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jb.cce.SessionSerializer
import org.jb.cce.info.EvaluationInfo
import org.jb.cce.info.FileSessionsInfo
import java.io.FileWriter
import java.nio.file.Paths

class SessionsStorage(private val storageDir: String, val evaluationType: String) {
    companion object {
        private const val pathsListFile = "files.json"
        private const val configFile = "config.json"
        private val gson = Gson()
        private val sessionSerializer = SessionSerializer()
    }
    private var filesCounter = 0
    private var sessionFiles: MutableMap<String, String> = mutableMapOf()
    private val typeFolder = Paths.get(storageDir, evaluationType)
    private val keyValueStorage = FileArchivesStorage(typeFolder.toString())

    fun saveSessions(sessionsInfo: FileSessionsInfo) {
        val json = sessionSerializer.serialize(sessionsInfo)
        val archivePath = keyValueStorage.save("${Paths.get(sessionsInfo.filePath).fileName}($filesCounter).json", json)
        sessionFiles[sessionsInfo.filePath] = Paths.get(storageDir).relativize(Paths.get(archivePath)).toString()
        filesCounter++
    }

    fun saveEvaluationInfo(info: EvaluationInfo) {
        val filesJson = gson.toJson(sessionFiles)
        FileWriter(Paths.get(storageDir, pathsListFile).toString()).use { it.write(filesJson) }
        val configJson = sessionSerializer.serializeConfig(info)
        FileWriter(Paths.get(storageDir, configFile).toString()).use { it.write(configJson) }
    }

    fun getSessionFiles(): List<Pair<String, String>> {
        val json = Paths.get(storageDir, pathsListFile).toFile().readText()
        val type = object : TypeToken<MutableMap<String, String>>() {}.type
        sessionFiles = gson.fromJson(json, type)
        for (path in sessionFiles.keys) sessionFiles[path] = Paths.get(storageDir).resolve(sessionFiles[path]!!).toString()
        return sessionFiles.entries.map { it.toPair() }
    }

    fun getSessions(path: String): FileSessionsInfo {
        val sessionsPath = sessionFiles[path] ?: throw NoSuchElementException()
        return sessionSerializer.deserialize(keyValueStorage.get(sessionsPath))
    }
}
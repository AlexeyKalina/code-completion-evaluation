package org.jb.cce

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jb.cce.info.EvaluationInfo
import org.jb.cce.info.FileSessionsInfo
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

class SessionsStorage(val storageDir: String, val evaluationType: String) {
    companion object {
        private const val pathsListFile = "files.json"
        private const val configFile = "config.json"
        private val gson = Gson()
        private val sessionSerializer = SessionSerializer()
    }
    private var filesCounter = 0
    private var sessionFiles: MutableMap<String, String> = mutableMapOf()
    private val typeFolder = Paths.get(storageDir, evaluationType)

    fun saveSessions(sessionsInfo: FileSessionsInfo) {
        if (!typeFolder.toFile().exists()) Files.createDirectories(typeFolder)
        val json = sessionSerializer.serialize(sessionsInfo)
        val dataPath = Paths.get(typeFolder.toString(), "${Paths.get(sessionsInfo.filePath).fileName}($filesCounter).json")
        FileWriter(dataPath.toString()).use { it.write(json) }
        sessionFiles[sessionsInfo.filePath] = Paths.get(storageDir).relativize(dataPath).toString()
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
        val json = FileReader(sessionsPath).use { it.readText() }
        return sessionSerializer.deserialize(json)
    }
}
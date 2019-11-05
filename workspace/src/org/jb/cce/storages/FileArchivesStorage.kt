package org.jb.cce.storages

import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class FileArchivesStorage(private val storageDir: String) : KeyValueStorage<String, String> {
    init {
        val storagePath = Paths.get(storageDir)
        if (!Files.exists(storagePath)) Files.createDirectories(storagePath)
    }

    override fun get(key: String): String {
        return InputStreamReader(GZIPInputStream(FileInputStream(key))).use {
            it.readText()
        }
    }

    override fun getKeys(): List<String> {
        return File(storageDir).listFiles()?.map { it.path } ?: emptyList()
    }

    override fun save(baseKey: String, value: String): String {
        val output = Files.createFile(Paths.get(storageDir, "$baseKey.gz")).toFile()
        OutputStreamWriter(GZIPOutputStream(FileOutputStream(output))).use {
            it.write(value)
        }
        return output.path
    }
}
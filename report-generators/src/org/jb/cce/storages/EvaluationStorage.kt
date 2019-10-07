package org.jb.cce.storages

import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.*
import java.io.FileInputStream
import java.io.File.separator
import java.io.FileOutputStream


abstract class EvaluationStorage(protected val storageDir: String) {
    fun compress() {
        val storage = File(storageDir)
        val output = Files.createFile(Paths.get("$storageDir.tar.gz")).toFile()
        TarArchiveOutputStream(GZIPOutputStream(BufferedOutputStream(FileOutputStream(output)))).use {
            it.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
            it.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            addFilesToCompression(it, storage, ".")
        }
        storage.deleteRecursively()
    }

    private fun addFilesToCompression(out: TarArchiveOutputStream, file: File, dir: String) {
        val entry = dir + separator + file.name
        if (file.isFile) {
            out.putArchiveEntry(TarArchiveEntry(file, entry))
            BufferedInputStream(FileInputStream(file)).use { it.copyTo(out) }
            out.closeArchiveEntry()
        } else if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    addFilesToCompression(out, child, entry)
                }
            }
        }
    }

    fun decompress() {
        val storage = File(storageDir)
        val baseDir = storage.parent
        val input = Paths.get("$storageDir.tar.gz").toFile()
        TarArchiveInputStream(GZIPInputStream(FileInputStream(input))).use { fin ->
            var entry = fin.nextTarEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    entry = fin.nextTarEntry
                    continue
                }
                val curFile = File(baseDir, entry.name)
                val parent = curFile.parentFile
                if (!parent.exists()) {
                    parent.mkdirs()
                }
                fin.copyTo(FileOutputStream(curFile))
                entry = fin.nextTarEntry
            }
        }
        if (!storage.exists()) Files.createDirectories(storage.toPath())
        input.delete()
    }
}
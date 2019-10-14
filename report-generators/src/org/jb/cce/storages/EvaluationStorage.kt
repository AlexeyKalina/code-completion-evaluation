package org.jb.cce.storages

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.*
import java.io.File.separator
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

abstract class EvaluationStorage(protected val storageDir: String) {
    protected fun compress() {
        val storage = File(storageDir)
        val output = Files.createFile(Paths.get("$storageDir.tar.gz")).toFile()
        TarArchiveOutputStream(GZIPOutputStream(BufferedOutputStream(FileOutputStream(output)))).use {
            it.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
            it.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            addFilesToCompression(it, storage, ".")
        }
        storage.deleteRecursively()
    }

    protected fun saveFile(path: String, text: String): String {
        val output = Files.createFile(Paths.get("$path.gz")).toFile()
        OutputStreamWriter(GZIPOutputStream(FileOutputStream(output))).use {
            it.write(text)
        }
        return output.path
    }

    protected fun readFile(path: String): String {
        return InputStreamReader(GZIPInputStream(FileInputStream(path))).use {
            it.readText()
        }
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
}
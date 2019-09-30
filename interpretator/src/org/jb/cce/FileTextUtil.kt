package org.jb.cce

import java.security.MessageDigest
import com.github.difflib.DiffUtils
import java.lang.StringBuilder


object FileTextUtil {
    fun computeChecksum(text: String): String {
        val sha = MessageDigest.getInstance("SHA-256")
        val digest = sha.digest(text.toByteArray())
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    fun getDiff(text1: String, text2: String): String {
        val patch = DiffUtils.diff(text1.lines(), text2.lines())
        val sb = StringBuilder()
        for (delta in patch.deltas) {
            sb.appendln(delta)
        }
        return sb.toString()
    }
}
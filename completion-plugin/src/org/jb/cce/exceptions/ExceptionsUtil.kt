package org.jb.cce.exceptions

import java.io.PrintWriter
import java.io.StringWriter

object ExceptionsUtil {
    fun stackTraceToString(e: Throwable): String {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}
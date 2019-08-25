package org.jb.cce

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream

class HtmlPrintStream(private val stream: OutputStream = ByteArrayOutputStream()) : PrintStream(stream) {
    override fun println(text: String) {
        super.println("<p>$text</p>")
    }

    override fun toString(): String {
        return stream.toString()
    }
}
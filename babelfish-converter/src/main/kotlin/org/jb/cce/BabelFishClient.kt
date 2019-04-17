package org.jb.cce

import com.sun.jna.*
import java.util.*

class BabelFishClient(libraryPath: String, private val endpoint: String) {
    private val client = Native.loadLibrary(libraryPath, GoBabelFishClient::class.java) as GoBabelFishClient

    fun parse(filePath: String): String {
        return client.Parse(getGoStr(filePath), getGoStr(endpoint))
    }

    private fun getGoStr(value: String): GoBabelFishClient.GoString.ByValue {
        val str = GoBabelFishClient.GoString.ByValue()
        str.p = value
        str.n = str.p!!.length.toLong()
        return str
    }

    interface GoBabelFishClient : Library {
        open class GoString : Structure() {
            class ByValue : GoString(), Structure.ByValue {}

            public override fun getFieldOrder(): MutableList<Any?> {
                return Arrays.asList("p", "n")
            }

            @JvmField
            var p: String? = null
            @JvmField
            var n: Long = 0
        }

        fun Parse(file: GoString.ByValue, endpoint: GoString.ByValue): String
    }
}
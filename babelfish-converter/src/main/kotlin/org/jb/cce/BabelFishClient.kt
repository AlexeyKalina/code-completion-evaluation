package org.jb.cce

import com.sun.jna.*
import org.jb.cce.exceptions.BabelFishClientException
import org.jb.cce.uast.Language
import java.util.*

class BabelFishClient(private val endpoint: String = "0.0.0.0:9432") {
    private companion object {
        private val client = Native.loadLibrary("bblfsh_client", GoBabelFishClient::class.java) as GoBabelFishClient
    }

    fun parse(text: String, language: Language): String {
        if (text.isBlank()) return "{}"
        val result = client.Parse(getGoStr(text), getGoStr(language.name), getGoStr(endpoint)).getString(0)
        checkErrors(result)
        return result
    }

    private fun checkErrors(result: String) {
        val checkError = { prefix: String, explanation: String ->
            if (result.startsWith(prefix)) throw BabelFishClientException("$explanation: ${result.substring(prefix.length)}")
        }
        checkError("ERROR-0", "BabelFish daemon error")
        checkError("ERROR-1", "File parsing error")
        checkError("ERROR-2", "Marshaling to JSON error")
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

        fun Parse(text: GoString.ByValue, language: GoString.ByValue, endpoint: GoString.ByValue): Pointer
    }
}
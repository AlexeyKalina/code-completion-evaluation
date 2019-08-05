package org.jb.cce

import java.util.*

class Session(val offset: Int,
              val expectedText: String,
              val tokenType: TokenType) {
    private val _lookups = mutableListOf<Lookup>()

    val lookups: List<Lookup>
        get() = _lookups

    val id = UUID.randomUUID()!!
    var success = false

    fun addLookup(lookup: Lookup) = _lookups.add(lookup)
}

enum class TokenType {
    METHOD_CALL,
    VARIABLE,
    FIELD
}
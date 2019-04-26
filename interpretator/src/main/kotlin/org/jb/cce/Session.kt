package org.jb.cce

import java.util.*

class Session(val offset: Int,
              val expectedText: String) {
    private val _lookups = mutableListOf<Lookup>()

    val lookups: List<Lookup>
        get() = _lookups

    val id = UUID.randomUUID()!!

    fun addLookup(lookup: Lookup) = _lookups.add(lookup)
}
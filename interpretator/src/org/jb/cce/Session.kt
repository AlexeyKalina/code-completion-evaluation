package org.jb.cce

import org.jb.cce.uast.NodeProperties
import java.util.*

class Session(val offset: Int,
              val expectedText: String,
              val properties: NodeProperties) {
    private val _lookups = mutableListOf<Lookup>()

    val lookups: List<Lookup>
        get() = _lookups

    val id = UUID.randomUUID()!!
    var success = false

    fun addLookup(lookup: Lookup) = _lookups.add(lookup)
}
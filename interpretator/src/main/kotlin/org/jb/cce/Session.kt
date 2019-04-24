package org.jb.cce

import java.util.*

class Session(val offset: Int,
              val expectedText: String) {
    val lookups: MutableList<Lookup> = mutableListOf()
    val id = UUID.randomUUID()!!
}
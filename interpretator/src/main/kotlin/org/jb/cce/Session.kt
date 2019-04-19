package org.jb.cce

import java.util.*

class Session(val offset: Int,
              val completion: String,
              val lookups: List<String>) {
    val id = UUID.randomUUID()!!
}
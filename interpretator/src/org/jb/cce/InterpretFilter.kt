package org.jb.cce

interface InterpretFilter {
    companion object {
        fun default(): InterpretFilter = object : InterpretFilter {
            override fun shouldCompleteToken(): Boolean = true
        }
    }

    fun shouldCompleteToken(): Boolean
}
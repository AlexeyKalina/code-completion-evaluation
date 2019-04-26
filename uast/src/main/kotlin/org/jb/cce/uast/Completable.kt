package org.jb.cce.uast

interface Completable {
    fun getText(): String
    fun getOffset(): Int
    fun getLength(): Int
}
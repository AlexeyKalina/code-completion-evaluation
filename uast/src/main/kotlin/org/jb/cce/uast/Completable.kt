package org.jb.cce.uast

interface Completable : EvaluationRoot {
    fun getText(): String
    fun getOffset(): Int
    fun getLength(): Int
    override fun contains(offset: Int) = getOffset() <= offset && offset <= getOffset() + getLength()
}
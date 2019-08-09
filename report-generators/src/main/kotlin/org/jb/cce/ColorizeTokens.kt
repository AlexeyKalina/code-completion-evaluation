package org.jb.cce

interface ColorizeTokens<T> {
    val middleCountLookups: Int
    val goodColor: T
    val middleColor: T
    val badColor: T
    val absentColor: T

    fun getColor(session: Session?): T {
        return when {
            session == null -> absentColor
            !session.lookups.last().suggestions.any{ it.text == session.expectedText } -> badColor
            session.lookups.last().suggestions.size < middleCountLookups ||
                    session.lookups.last().suggestions.subList(0, middleCountLookups).any{ it.text == session.expectedText } -> goodColor
            else -> middleColor
        }
    }
}
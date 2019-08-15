package org.jb.cce

interface ReportColors<T> {
    val middleCountLookups: Int
    val goodColor: T
    val middleColor: T
    val badColor: T
    val absentColor: T
}

fun<T> getColor(session: Session?, colors: ReportColors<T>): T {
    return when {
        session == null -> colors.absentColor
        !session.lookups.last().suggestions.any{ it.text == session.expectedText } -> colors.badColor
        session.lookups.last().suggestions.size < colors.middleCountLookups ||
                session.lookups.last().suggestions.subList(0, colors.middleCountLookups).any{ it.text == session.expectedText } -> colors.goodColor
        else -> colors.middleColor
    }
}
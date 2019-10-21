package org.jb.cce

interface ReportColors<T> {
    companion object {
        fun<T> getColor(session: Session?, colors: ReportColors<T>, prefixLength: Int): T {
            return when {
                session == null || session.lookups.size <= prefixLength -> colors.absentColor
                !session.lookups[prefixLength].suggestions.any{ it.text == session.expectedText } -> colors.badColor
                session.lookups[prefixLength].suggestions.size < colors.middleCountLookups ||
                        session.lookups[prefixLength].suggestions.subList(0, colors.middleCountLookups).any{ it.text == session.expectedText } -> colors.goodColor
                else -> colors.middleColor
            }
        }
    }

    val middleCountLookups: Int
    val goodColor: T
    val middleColor: T
    val badColor: T
    val absentColor: T
}
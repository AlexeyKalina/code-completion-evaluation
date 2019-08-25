package org.jb.cce.highlighter

import org.jb.cce.ReportColors
import java.awt.Color

object HighlightColors : ReportColors<Color> {
    override val middleCountLookups = 3
    override val absentColor = Color(112, 170, 255)
    override val goodColor = Color(188, 245, 188)
    override val middleColor = Color(255, 250, 205)
    override val badColor = Color(255, 153, 153)
}
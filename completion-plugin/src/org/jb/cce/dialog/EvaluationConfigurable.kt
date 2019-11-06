package org.jb.cce.dialog

import org.jb.cce.Config
import javax.swing.JPanel

interface EvaluationConfigurable {
    fun createPanel(previousState: Config): JPanel
    fun configure(builder: Config.Builder)
}

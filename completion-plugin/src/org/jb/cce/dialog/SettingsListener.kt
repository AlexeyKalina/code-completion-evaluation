package org.jb.cce.dialog

import org.jb.cce.uast.Language
import java.util.*

interface SettingsListener : EventListener {
    fun languageChanged(language: Language) {}
    fun allTokensChanged(selected: Boolean) {}
}
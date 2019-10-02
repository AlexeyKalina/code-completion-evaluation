package org.jb.cce.filter

import javax.swing.JPanel

interface EvaluationFilterConfiguration {
  interface Configurable {
    val panel: JPanel
    fun build(): EvaluationFilter
    fun isLanguageSupported(languageName: String): Boolean
  }

  val id: String

  val description: String

  fun createConfigurable(): Configurable

  fun isLanguageSupported(languageName: String): Boolean

  fun buildFromJson(json: Any?): EvaluationFilter

  fun defaultFilter(): EvaluationFilter
}

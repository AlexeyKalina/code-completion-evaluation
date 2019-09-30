package org.jb.cce.filter

import org.jb.cce.uast.Language
import javax.swing.JPanel

interface EvaluationFilterConfiguration {
  interface Configurable {
    val panel: JPanel
    fun build(): EvaluationFilter
  }

  val id: String

  val description: String

  fun getConfigurable(): Configurable

  // says what languages support the filter
  fun supportedLanguages(): Set<Language>

  fun buildFromJson(json: Any): EvaluationFilter
}

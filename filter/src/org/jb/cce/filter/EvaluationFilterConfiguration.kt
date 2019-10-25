package org.jb.cce.filter

interface EvaluationFilterConfiguration {
  interface Configurable {
    fun build(): EvaluationFilter
  }

  val id: String

  val description: String

  fun createConfigurable(configurableBuilder: ConfigurableBuilder): Configurable = configurableBuilder.build()

  fun isLanguageSupported(languageName: String): Boolean

  fun buildFromJson(json: Any?): EvaluationFilter

  fun defaultFilter(): EvaluationFilter
}

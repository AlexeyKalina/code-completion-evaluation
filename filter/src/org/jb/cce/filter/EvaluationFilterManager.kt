package org.jb.cce.filter

import org.jb.cce.filter.impl.ArgumentFilterConfiguration
import org.jb.cce.filter.impl.PackageRegexFilterConfiguration
import org.jb.cce.filter.impl.StaticFilterConfiguration
import org.jb.cce.filter.impl.TypeFilterConfiguration
import org.jetbrains.annotations.TestOnly

object EvaluationFilterManager {
  private val id2Configuration: MutableMap<String, EvaluationFilterConfiguration> = mutableMapOf()

  init {
    register(TypeFilterConfiguration())
    register(ArgumentFilterConfiguration())
    register(StaticFilterConfiguration())
    register(PackageRegexFilterConfiguration())
  }

  fun getConfigurationById(id: String): EvaluationFilterConfiguration? = id2Configuration[id]

  fun getAllFilters(): List<EvaluationFilterConfiguration> = id2Configuration.values.toList()

  @TestOnly
  fun registerFilter(configuration: EvaluationFilterConfiguration) {
    register(configuration)
  }

  @TestOnly
  fun unregisterFilter(configuration: EvaluationFilterConfiguration) {
    id2Configuration.remove(configuration.id)
  }

  private fun register(configuration: EvaluationFilterConfiguration) {
    val old = id2Configuration[configuration.id]
    if (old != null) {
      System.err.println("Configuration with id [${old.id}] already created. " +
              "Classes: ${old.javaClass.canonicalName}, ${configuration.javaClass.canonicalName}")
      return
    }

    id2Configuration[configuration.id] = configuration
  }
}
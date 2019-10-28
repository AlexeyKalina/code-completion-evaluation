package org.jb.cce.filter

interface ConfigurableBuilder<T> {
    fun build(filterId: String): EvaluationFilterConfiguration.Configurable<T>
}
package org.jb.cce

import com.intellij.ui.layout.panel
import org.jb.cce.dialog.configurable.FilterUIConfigurableFactory
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.filter.EvaluationFilterManager
import org.jb.cce.util.ConfigFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FilterUIConfigurableFactoryTest {
    companion object {
        private const val TEST_FILTER_ID = "testFilter"
    }

    @Test
    fun `all filters have ui configurable`() = doTest { factory ->
        EvaluationFilterManager.getAllFilters().forEach {
            assertNotNull(factory.build(it.id))
        }
    }

    @Test
    fun `exception if filter hasn't UI configurable`() {
        val testFilterConfiguration = TestFilterConfiguration()
        try {
            EvaluationFilterManager.registerFilter(testFilterConfiguration)
            doTest { factory ->
                assertThrows<IllegalStateException> { factory.build(TEST_FILTER_ID) }
            }
        } finally {
            EvaluationFilterManager.unregisterFilter(testFilterConfiguration)
            assertNull(EvaluationFilterManager.getConfigurationById(TEST_FILTER_ID))
        }
    }

    @Test
    fun `exception if unknown filter`() = doTest { factory ->
        assertThrows<IllegalArgumentException> { factory.build("unknownFilter") }
    }

    private fun doTest(body: (FilterUIConfigurableFactory) -> Unit) {
        panel {
            val factory = FilterUIConfigurableFactory(ConfigFactory.defaultConfig(), this)
            body(factory)
        }
    }

    private class TestFilterConfiguration() : EvaluationFilterConfiguration {
        override val id: String = TEST_FILTER_ID
        override val description: String = "Should be used only in tests"
        override fun isLanguageSupported(languageName: String): Boolean = true
        override fun buildFromJson(json: Any?): EvaluationFilter = EvaluationFilter.ACCEPT_ALL
        override fun defaultFilter(): EvaluationFilter = EvaluationFilter.ACCEPT_ALL
    }
}
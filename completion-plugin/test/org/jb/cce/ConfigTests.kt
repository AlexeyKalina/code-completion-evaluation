package org.jb.cce

import com.google.gson.Gson
import junit.framework.TestCase
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.impl.PackageRegexFilter
import org.jb.cce.filter.impl.PackageRegexFilterConfiguration
import org.jb.cce.filter.impl.StaticFilter
import org.jb.cce.filter.impl.StaticFilterConfiguration
import org.jb.cce.util.Config
import org.jb.cce.util.ConfigFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.lang.IllegalArgumentException
import java.nio.file.Path

@DisplayName("Check config serialized and deserialized correctly")
class ConfigTests {
    companion object {
        val gson = Gson()
        private const val FILENAME = "config.json"
    }

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `default config`() = doTest {}

    @Test
    fun `modify field`() = doTest { allTokens = true }

    @Test
    fun `with evaluation root`() = doTest {
        evaluationRoots.add(tempDir.resolve("project").resolve("subdir").toString())
    }

    @Test
    fun `with filters`() = doTest {
        filters[PackageRegexFilterConfiguration.id] = PackageRegexFilter("com\\.jetbrains\\..*")
        filters[StaticFilterConfiguration.id] = StaticFilter(false)
    }

    @Test
    fun `update completion type`() = doTest {
        completionType = CompletionType.SMART
    }

    @Test
    fun `create default config if it's absent`() {
        val path = tempDir.resolve(FILENAME)
        assertThrows<IllegalArgumentException> { ConfigFactory.load(path) }
        assertConfigsTheSame("Default config should be created", ConfigFactory.defaultConfig(), ConfigFactory.load(path))
    }

    private fun doTest(init: Config.Builder.() -> Unit) {
        val before = Config.build(tempDir.resolve("project").toString(), "Java", init)
        ConfigFactory.save(before, tempDir, FILENAME)
        val after = ConfigFactory.load(tempDir.resolve(FILENAME))
        assertConfigsTheSame("Stored and restored configs should be equal", before, after)
    }

    private fun assertConfigsTheSame(message: String, expected: Config, actual: Config) {
        TestCase.assertEquals(message, gson.toJson(expected), gson.toJson(actual))
    }
}
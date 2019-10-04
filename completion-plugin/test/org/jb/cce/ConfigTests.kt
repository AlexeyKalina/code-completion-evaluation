package org.jb.cce

import com.google.gson.Gson
import junit.framework.TestCase
import org.jb.cce.util.ConfigFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ConfigTests {
    companion object {
        val gson = Gson()
    }

    @DisplayName("Check default config was stored and restored correctly")
    @Test
    fun defaultConfig(@TempDir tempDir: Path) {
        val configPath = tempDir.resolve("config.json").toAbsolutePath().toString()
        val before = ConfigFactory.save(configPath)
        val after = ConfigFactory.load(configPath)
        TestCase.assertEquals("Stored and restored configs are not equal", gson.toJson(before), gson.toJson(after))
    }
}